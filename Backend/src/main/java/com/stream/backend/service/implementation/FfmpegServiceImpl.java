package com.stream.backend.service.implementation;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stream.backend.entity.FfmpegStat;
import com.stream.backend.service.FfmpegService;

@Service
public class FfmpegServiceImpl implements FfmpegService {

    @Value("${stream.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${stream.youtube.rtmp}")
    private String youtubeRtmp;

    // @Value("${stream.demo.video}")
    // private String demoVideoPath;

    /**
     * Nếu true: ưu tiên -c copy (remux) để đạt speed cao.
     * Nếu copy fail hoặc YouTube reject thì fallback encode.
     */
    @Value("${stream.ffmpeg.preferCopy:true}")
    private boolean preferCopy;

    /** lưu process theo streamKey */
    private final Map<String, Process> processMap = new ConcurrentHashMap<>();

    /** snapshot realtime cho FE */
    private final Map<String, FfmpegStat> statMap = new ConcurrentHashMap<>();

    @Override
    public void startStream(String videoPath, String rtmpUrl, String streamKey) {

        // if (videoPath == null || videoPath.isBlank()) {
        //     videoPath = demoVideoPath;
        // }
        if (videoPath == null || videoPath.isBlank()) {
            throw new RuntimeException("Video path is empty. Please config stream.demo.video");
        }

        if (rtmpUrl == null || rtmpUrl.isBlank()) {
            rtmpUrl = youtubeRtmp;
        }

        String fullRtmp = rtmpUrl.endsWith("/")
                ? rtmpUrl + streamKey
                : rtmpUrl + "/" + streamKey;

        stopStream(streamKey); // đảm bảo không chạy trùng

        // init stat để FE không null
        FfmpegStat init = new FfmpegStat();
        init.updatedAt = System.currentTimeMillis();
        statMap.put(streamKey, init);

        // 1) thử copy trước (nếu bật)
        if (preferCopy) {
            boolean started = startCopyStream(videoPath, fullRtmp, streamKey);
            if (started) return; // copy OK
        }

        // 2) fallback: encode như bạn đang dùng (an toàn hơn)
        startEncodeStream(videoPath, fullRtmp, streamKey);
    }

    /**
     * Chạy FFmpeg remux với -c copy (nhẹ nhất).
     * Return true nếu process start OK; nếu fail thì return false để fallback encode.
     */
    private boolean startCopyStream(String videoPath, String fullRtmp, String streamKey) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);

            // INPUT
            cmd.add("-re");
            cmd.add("-stream_loop");
            cmd.add("-1");
            cmd.add("-i");
            cmd.add(videoPath);

            // COPY (remux)
            cmd.add("-c:v");
            cmd.add("copy");
            cmd.add("-c:a");
            cmd.add("copy");

            // OUTPUT
            cmd.add("-f");
            cmd.add("flv");
            cmd.add("-flvflags");
            cmd.add("no_duration_filesize");

            // PROGRESS
            cmd.add("-progress");
            cmd.add("pipe:1");
            cmd.add("-nostats");

            cmd.add(fullRtmp);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();
            processMap.put(streamKey, process);

            new Thread(() -> readProgress(streamKey, process)).start();

            System.out.println("[FFMPEG] Started COPY stream: " + streamKey);
            return true;

        } catch (IOException e) {
            System.out.println("[FFMPEG] COPY start failed, fallback to ENCODE. Reason: " + e.getMessage());
            return false;
        }
    }

    /**
     * Encode fallback: 720p30, superfast, VBV.
     * (Bạn có thể chỉnh thông số tại đây nếu cần)
     */
    private void startEncodeStream(String videoPath, String fullRtmp, String streamKey) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);

            // INPUT
            cmd.add("-re");
            cmd.add("-stream_loop");
            cmd.add("-1");
            cmd.add("-i");
            cmd.add(videoPath);

            // VIDEO
            cmd.add("-vf");
            cmd.add("scale=1280:720:flags=bilinear");

            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("superfast");
            cmd.add("-profile:v");
            cmd.add("high");
            cmd.add("-level");
            cmd.add("4.1");
            cmd.add("-pix_fmt");
            cmd.add("yuv420p");

            // GOP 2s
            cmd.add("-g");
            cmd.add("60");
            cmd.add("-keyint_min");
            cmd.add("60");
            cmd.add("-sc_threshold");
            cmd.add("0");

            // VBV
            cmd.add("-b:v");
            cmd.add("3000k");
            cmd.add("-maxrate");
            cmd.add("3000k");
            cmd.add("-bufsize");
            cmd.add("6000k");

            // CFR
            cmd.add("-r");
            cmd.add("30");
            cmd.add("-vsync");
            cmd.add("cfr");

            // AUDIO
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("128k");
            cmd.add("-ar");
            cmd.add("44100");
            cmd.add("-ac");
            cmd.add("2");

            // OUTPUT
            cmd.add("-f");
            cmd.add("flv");
            cmd.add("-flvflags");
            cmd.add("no_duration_filesize");

            // PROGRESS
            cmd.add("-progress");
            cmd.add("pipe:1");
            cmd.add("-nostats");

            cmd.add(fullRtmp);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();
            processMap.put(streamKey, process);

            new Thread(() -> readProgress(streamKey, process)).start();

            System.out.println("[FFMPEG] Started ENCODE stream: " + streamKey);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start FFmpeg ENCODE", e);
        }
    }

    private void readProgress(String streamKey, Process process) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;

            // dùng object riêng để update dần
            FfmpegStat stat = new FfmpegStat();

            while ((line = br.readLine()) != null) {
                System.out.println("[FFMPEG] " + line);

                if (!line.contains("=")) continue;

                String[] kv = line.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String val = kv[1].trim();

                switch (key) {
                    case "frame" -> stat.frame = parseLong(val);
                    case "fps" -> stat.fps = parseDouble(val);
                    case "bitrate" -> stat.bitrate = val;
                    case "speed" -> stat.speed = val;
                    case "out_time" -> stat.time = val;

                    // các key này có thể có (tuỳ build ffmpeg)
                    // nếu FfmpegStat bạn không có field thì cứ bỏ các dòng này
                    case "total_size" -> stat.size = val;           // nếu bạn đang dùng size dạng String
                    // case "dup_frames" -> stat.dupFrames = parseLong(val);
                    // case "drop_frames" -> stat.dropFrames = parseLong(val);
                }

                stat.updatedAt = System.currentTimeMillis();
                statMap.put(streamKey, stat);
            }
        } catch (IOException ignored) {
        } finally {
            processMap.remove(streamKey);
            statMap.remove(streamKey);
        }
    }

    @Override
    public void stopStream(String streamKey) {
        if (streamKey == null || streamKey.isBlank()) return;

        Process process = processMap.remove(streamKey);
        statMap.remove(streamKey);

        if (process == null) return;

        try {
            // stop mềm
            OutputStream os = process.getOutputStream();
            os.write("q\n".getBytes());
            os.flush();

            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            process.destroyForcibly();
        }

        System.out.println("[FFMPEG] Stopped stream: " + streamKey);
    }

    @Override
    public FfmpegStat getLatestStat(String streamKey) {
        return statMap.get(streamKey);
    }

    private static long parseLong(String v) {
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return 0L; }
    }

    private static double parseDouble(String v) {
        try { return Double.parseDouble(v.trim()); } catch (Exception e) { return 0.0; }
    }
}
