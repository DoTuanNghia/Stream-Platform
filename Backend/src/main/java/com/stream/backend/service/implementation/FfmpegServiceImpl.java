package com.stream.backend.service.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Lưu process FFmpeg theo streamKey để sau còn stop được
    private final Map<String, Process> processMap = new ConcurrentHashMap<>();

    // NEW: lưu snapshot thông số theo streamKey
    private final Map<String, FfmpegStat> statMap = new ConcurrentHashMap<>();

    // Parse dòng progress phổ biến của ffmpeg
    // frame= 245 fps=5.2 q=23.0 size= 604KiB time=00:00:40.50 bitrate= 122.1kbits/s
    // speed=0.863x
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
            "frame=\\s*(\\d+)\\s+fps=\\s*([0-9.]+)\\s+q=\\s*([0-9.]+)\\s+size=\\s*(\\S+)\\s+time=(\\S+)\\s+bitrate=\\s*(\\S+)\\s+speed=(\\S+)");

    @Override
    public void startStream(String videoPath, String rtmpUrl, String streamKey) {
        // Nếu không truyền videoPath (hoặc rỗng) thì dùng video demo trong config
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

        // Nếu đã có process với streamKey này thì dừng nó trước
        Process old = processMap.get(streamKey);
        if (old != null && old.isAlive()) {
            old.destroy();
            try {
                old.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        final String keyForMap = streamKey; // dùng trong lambda

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);

            // ===== INPUT =====
            // đọc realtime + loop vô hạn
            cmd.add("-re");
            cmd.add("-stream_loop");
            cmd.add("-1");

            cmd.add("-i");
            cmd.add(videoPath);

            // ===== VIDEO =====
            // scale + ép fps để ổn định ingest
            cmd.add("-vf");
            cmd.add("scale=1280:720,fps=30");

            // encoder CPU
            cmd.add("-c:v");
            cmd.add("libx264");

            // preset (giữ veryfast, nhiều stream có thể hạ superfast)
            cmd.add("-preset");
            cmd.add("veryfast");

            // giảm latency
            cmd.add("-tune");
            cmd.add("zerolatency");

            // tương thích youtube
            cmd.add("-pix_fmt");
            cmd.add("yuv420p");

            // ===== GOP =====
            // 2s keyframe (30fps * 2)
            cmd.add("-g");
            cmd.add("60");
            cmd.add("-keyint_min");
            cmd.add("60");
            cmd.add("-sc_threshold");
            cmd.add("0");

            // ===== RATE CONTROL (CBR THẬT) =====
            cmd.add("-b:v");
            cmd.add("3000k");
            cmd.add("-minrate");
            cmd.add("3000k");
            cmd.add("-maxrate");
            cmd.add("3000k");
            cmd.add("-bufsize");
            cmd.add("6000k");

            // ÉP CBR + padding để bitrate KHÔNG tụt
            cmd.add("-x264-params");
            cmd.add("nal-hrd=cbr:filler=1:force-cfr=1");

            // ép fps đầu ra (CFR)
            cmd.add("-r");
            cmd.add("30");

            // ===== AUDIO =====
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("128k");
            cmd.add("-ar");
            cmd.add("44100");
            cmd.add("-ac");
            cmd.add("2");

            // ===== OUTPUT =====
            cmd.add("-f");
            cmd.add("flv");

            // tránh giật bitrate khi push RTMP
            cmd.add("-flvflags");
            cmd.add("no_duration_filesize");

            cmd.add(fullRtmp);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // gộp stderr vào stdout
            Process process = pb.start();

            // LƯU process vào map để sau này stop được
            processMap.put(keyForMap, process);

            // init stat để FE không bị null
            FfmpegStat init = new FfmpegStat();
            init.updatedAt = System.currentTimeMillis();
            statMap.put(keyForMap, init);

            // In log ffmpeg ra console + parse snapshot
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFMPEG] " + line);

                        FfmpegStat parsed = parseProgressLine(line);
                        if (parsed != null) {
                            parsed.updatedAt = System.currentTimeMillis();
                            statMap.put(keyForMap, parsed);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Khi process tự kết thúc (video hết / lỗi) thì xoá khỏi map
                    processMap.remove(keyForMap);
                    statMap.remove(keyForMap);
                }
            }).start();

            System.out.println("Started FFmpeg process for stream key: " + streamKey);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start FFmpeg process", e);
        }
    }

    @Override
    public void stopStream(String streamKey) {
        if (streamKey == null || streamKey.isBlank()) {
            return;
        }

        Process process = processMap.remove(streamKey);
        statMap.remove(streamKey);

        if (process == null) {
            System.out.println("No FFmpeg process found for stream key: " + streamKey);
            return;
        }

        if (process.isAlive()) {
            process.destroy(); // gửi tín hiệu dừng mềm
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly(); // nếu không dừng thì kill mạnh
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Stopped FFmpeg process for stream key: " + streamKey);
    }

    @Override
    public FfmpegStat getLatestStat(String streamKey) {
        if (streamKey == null || streamKey.isBlank())
            return null;
        return statMap.get(streamKey);
    }

    private FfmpegStat parseProgressLine(String line) {
        if (line == null)
            return null;

        Matcher m = PROGRESS_PATTERN.matcher(line);
        if (!m.find())
            return null;

        try {
            FfmpegStat s = new FfmpegStat();
            s.frame = Long.parseLong(m.group(1));
            s.fps = Double.parseDouble(m.group(2));
            s.q = Double.parseDouble(m.group(3));
            s.size = m.group(4);
            s.time = m.group(5);
            s.bitrate = m.group(6);
            s.speed = m.group(7);
            return s;
        } catch (Exception ex) {
            return null;
        }
    }
}
