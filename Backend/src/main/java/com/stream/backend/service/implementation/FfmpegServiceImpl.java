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

    @Value("${stream.ffmpeg.preferCopy:true}")
    private boolean preferCopy;

    /** lưu process theo streamKey */
    private final Map<String, Process> processMap = new ConcurrentHashMap<>();

    /** snapshot realtime cho FE */
    private final Map<String, FfmpegStat> statMap = new ConcurrentHashMap<>();

    @Override
    public void startStream(String videoPath, String rtmpUrl, String streamKey) {

        if (streamKey == null || streamKey.isBlank()) {
            throw new RuntimeException("STREAM_KEY_EMPTY");
        }

        if (videoPath == null || videoPath.isBlank()) {
            throw new RuntimeException("Video path is empty.");
        }

        // validate file local (không áp dụng với URL)
        if (isLocalFile(videoPath) && !new File(videoPath).exists()) {
            throw new RuntimeException("INPUT_NOT_FOUND: " + videoPath);
        }

        if (rtmpUrl == null || rtmpUrl.isBlank()) {
            rtmpUrl = youtubeRtmp;
        }

        String fullRtmp = rtmpUrl.endsWith("/") ? (rtmpUrl + streamKey) : (rtmpUrl + "/" + streamKey);

        stopStream(streamKey); // đảm bảo không chạy trùng

        // init stat để FE không null
        FfmpegStat init = new FfmpegStat();
        init.updatedAt = System.currentTimeMillis();
        statMap.put(streamKey, init);

        // 1) thử copy trước (nếu bật)
        if (preferCopy) {
            boolean started = startCopyStream(videoPath, fullRtmp, streamKey);
            if (started) return;
        }

        // 2) fallback encode (nếu encode fail -> throw)
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
            cmd.add("-stats_period");
            cmd.add("1");

            cmd.add(fullRtmp);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();
            processMap.put(streamKey, process);

            // kiểm tra start thật sự ok (chết sớm / log lỗi)
            assertProcessStartedOk(streamKey, process, 2000);

            new Thread(() -> readProgress(streamKey, process)).start();

            System.out.println("[FFMPEG] Started COPY stream: " + streamKey);
            return true;

        } catch (RuntimeException e) {
            // FAIL runtime -> fallback encode
            System.out.println("[FFMPEG] COPY start failed, fallback to ENCODE. Reason: " + e.getMessage());
            stopStream(streamKey);
            return false;

        } catch (IOException e) {
            System.out.println("[FFMPEG] COPY start failed, fallback to ENCODE. Reason: " + e.getMessage());
            stopStream(streamKey);
            return false;
        }
    }

    /**
     * Encode fallback: 720p30, superfast, VBV.
     * Nếu fail -> throw để session chuyển ERROR.
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

            // encode start fail -> throw
            assertProcessStartedOk(streamKey, process, 2000);

            new Thread(() -> readProgress(streamKey, process)).start();

            System.out.println("[FFMPEG] Started ENCODE stream: " + streamKey);

        } catch (IOException e) {
            stopStream(streamKey);
            throw new RuntimeException("Failed to start FFmpeg ENCODE", e);
        } catch (RuntimeException e) {
            stopStream(streamKey);
            throw e;
        }
    }

    private void readProgress(String streamKey, Process process) {
        FfmpegStat stat = new FfmpegStat();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println("[FFMPEG] " + line);

                // Parse STATS line
                if (line.contains("frame=") && line.contains("fps=")) {
                    parseStatsLineInto(stat, line);
                    stat.updatedAt = System.currentTimeMillis();
                    statMap.put(streamKey, cloneStat(stat));
                    continue;
                }

                // Parse PROGRESS: key=value
                if (!line.contains("=")) continue;

                String[] kv = line.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String val = kv[1].trim();

                switch (key) {
                    case "frame" -> stat.frame = parseLong(val);
                    case "stream_0_0_fps" -> stat.fps = parseDouble(val);
                    case "stream_0_0_q" -> stat.q = parseDouble(val);
                    case "bitrate" -> stat.bitrate = val;
                    case "speed" -> stat.speed = val;
                    case "out_time" -> stat.time = val;
                    case "total_size" -> stat.size = humanBytes(parseLong(val));
                    case "out_time_us" -> stat.outTimeMs = parseLong(val) / 1000L; // us -> ms
                    case "out_time_ms" -> stat.outTimeMs = parseLong(val) / 1000L; // treat as us -> ms
                    case "progress" -> {
                        if ((stat.fps == null || stat.fps == 0.0)
                                && stat.frame != null && stat.outTimeMs != null && stat.outTimeMs > 0) {
                            stat.fps = stat.frame / (stat.outTimeMs / 1000.0);
                        }
                        stat.updatedAt = System.currentTimeMillis();
                        statMap.put(streamKey, cloneStat(stat));
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            processMap.remove(streamKey);
            statMap.remove(streamKey);
        }
    }

    private static void parseStatsLineInto(FfmpegStat stat, String line) {
        try {
            Long frame = extractLongAfter(line, "frame=");
            if (frame != null) stat.frame = frame;

            Double fps = extractDoubleAfter(line, "fps=");
            if (fps != null) stat.fps = fps;

            Double q = extractDoubleAfter(line, "q=");
            if (q != null) stat.q = q;

            String bitrate = extractTokenAfter(line, "bitrate=");
            if (bitrate != null) stat.bitrate = bitrate;

            String speed = extractTokenAfter(line, "speed=");
            if (speed != null) stat.speed = speed;

            String time = extractTokenAfter(line, "time=");
            if (time != null) stat.time = time;

            String size = extractTokenAfter(line, "size=");
            if (size != null) stat.size = size;

        } catch (Exception ignored) {
        }
    }

    private static String extractTokenAfter(String line, String key) {
        int i = line.indexOf(key);
        if (i < 0) return null;
        String tail = line.substring(i + key.length()).trim();
        if (tail.isEmpty()) return null;
        int sp = tail.indexOf(' ');
        return (sp < 0) ? tail : tail.substring(0, sp).trim();
    }

    private static Long extractLongAfter(String line, String key) {
        String t = extractTokenAfter(line, key);
        if (t == null) return null;
        try { return Long.parseLong(t.trim()); } catch (Exception e) { return null; }
    }

    private static Double extractDoubleAfter(String line, String key) {
        String t = extractTokenAfter(line, key);
        if (t == null) return null;
        try { return Double.parseDouble(t.trim()); } catch (Exception e) { return null; }
    }

    private static FfmpegStat cloneStat(FfmpegStat s) {
        FfmpegStat c = new FfmpegStat();
        c.frame = s.frame;
        c.fps = s.fps;
        c.q = s.q;
        c.size = s.size;
        c.bitrate = s.bitrate;
        c.time = s.time;
        c.speed = s.speed;
        c.outTimeMs = s.outTimeMs;
        c.updatedAt = s.updatedAt;
        return c;
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        double kib = bytes / 1024.0;
        if (kib < 1024) return String.format(java.util.Locale.US, "%.0fKiB", kib);
        double mib = kib / 1024.0;
        if (mib < 1024) return String.format(java.util.Locale.US, "%.1fMiB", mib);
        double gib = mib / 1024.0;
        return String.format(java.util.Locale.US, "%.2fGiB", gib);
    }

    @Override
    public void stopStream(String streamKey) {
        if (streamKey == null || streamKey.isBlank()) return;

        Process process = processMap.remove(streamKey);
        statMap.remove(streamKey);

        if (process == null) return;

        try {
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

    private static boolean isUrl(String s) {
        if (s == null) return false;
        String x = s.trim().toLowerCase();
        return x.startsWith("http://") || x.startsWith("https://");
    }

    private static boolean isLocalFile(String s) {
        return s != null && !isUrl(s);
    }

    /**
     * Quan trọng: Bắt trường hợp ffmpeg chết ngay hoặc log báo lỗi trong vài giây đầu.
     * Vì redirectErrorStream(true) nên đọc từ process.getInputStream().
     *
     * NOTE: Hàm này đọc một phần output đầu, sau đó "đẩy" phần còn lại cho readProgress bằng cách
     * KHÔNG tạo reader thứ 2 song song. Ở đây ta chỉ dùng để detect fail sớm.
     * Thực tế, reader trong hàm này sẽ "ăn" vài dòng đầu, readProgress sẽ đọc tiếp phần còn lại (OK).
     */
    private void assertProcessStartedOk(String streamKey, Process process, long waitMs) {
        long end = System.currentTimeMillis() + waitMs;
        StringBuilder firstLines = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while (System.currentTimeMillis() < end) {
                if (!process.isAlive()) break;

                while (br.ready()) {
                    String line = br.readLine();
                    if (line == null) break;

                    System.out.println("[FFMPEG] " + line);
                    firstLines.append(line).append("\n");

                    String low = line.toLowerCase();

                    if (low.contains("error opening output")
                            || low.contains("error opening output files")
                            || low.contains("no such file or directory")
                            || low.contains("i/o error")
                            || low.contains("connection refused")
                            || low.contains("failed to resolve hostname")
                            || low.contains("server returned 4")
                            || low.contains("server returned 5")) {
                        throw new RuntimeException("FFMPEG_START_FAILED: " + line);
                    }
                }

                Thread.sleep(50);
            }

            if (!process.isAlive()) {
                int code = process.exitValue();
                String msg = firstLines.length() > 0 ? firstLines.toString().trim() : ("FFMPEG_EXIT_EARLY code=" + code);
                throw new RuntimeException("FFMPEG_START_FAILED: " + msg);
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("FFMPEG_START_CHECK_ERROR: " + e.getMessage(), e);
        }
    }
}
