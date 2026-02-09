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
            if (started)
                return;
        }

        // 2) fallback encode (nếu encode fail -> throw)
        startEncodeStream(videoPath, fullRtmp, streamKey);
    }

    /**
     * Chạy FFmpeg remux với -c copy (nhẹ nhất).
     * Return true nếu process start OK; nếu fail thì return false để fallback
     * encode.
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

            // Đợi một chút để process khởi động hoàn toàn trên Windows
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Bắt đầu đọc progress trong daemon thread (sẽ detect lỗi trong readProgress)
            Thread progressThread = new Thread(() -> readProgress(streamKey, process), "FFmpeg-Progress-" + streamKey);
            progressThread.setDaemon(true); // Daemon thread để không giữ JVM
            progressThread.start();

            // Kiểm tra process còn sống sau khi start (không đọc stream để tránh conflict)
            assertProcessStartedOk(streamKey, process, 2000);

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

            // Đợi một chút để process khởi động hoàn toàn trên Windows
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Bắt đầu đọc progress trong daemon thread (sẽ detect lỗi trong readProgress)
            Thread progressThread = new Thread(() -> readProgress(streamKey, process), "FFmpeg-Progress-" + streamKey);
            progressThread.setDaemon(true); // Daemon thread để không giữ JVM
            progressThread.start();

            // Kiểm tra process còn sống sau khi start (không đọc stream để tránh conflict)
            assertProcessStartedOk(streamKey, process, 2000);

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
        long startTime = System.currentTimeMillis();
        boolean earlyErrorDetected = false;
        BufferedReader br = null;
        InputStream is = null;

        // Throttle log và stats update mỗi 30 giây
        final long LOG_INTERVAL_MS = 30_000;
        long lastLogTime = 0;

        try {
            // Lấy InputStream từ process
            is = process.getInputStream();
            if (is == null) {
                System.err.println("[FFMPEG] Cannot get input stream for " + streamKey);
                return;
            }

            // Sử dụng UTF-8 encoding để đảm bảo đọc đúng trên Windows
            // Sử dụng buffer size lớn hơn để tránh block trên Windows
            br = new BufferedReader(
                    new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8),
                    8192);

            String line;
            long lastReadTime = System.currentTimeMillis();

            // Đọc stream cho đến khi process kết thúc hoặc stream đóng
            while (process.isAlive() || br.ready()) {
                try {
                    // Kiểm tra nếu có dữ liệu sẵn (non-blocking check)
                    if (br.ready()) {
                        line = br.readLine();
                        if (line == null) {
                            // Stream đã đóng
                            break;
                        }
                        lastReadTime = System.currentTimeMillis();
                    } else {
                        // Nếu không có dữ liệu, đợi một chút rồi kiểm tra lại
                        // Tránh busy-wait nhưng vẫn responsive
                        Thread.sleep(50);

                        // Nếu process đã chết và không có dữ liệu trong 1 giây, thoát
                        if (!process.isAlive() && (System.currentTimeMillis() - lastReadTime) > 1000) {
                            break;
                        }
                        continue;
                    }

                    // Không log mỗi dòng để tránh treo CMD
                    // Chỉ log khi có lỗi sớm (ở trên) hoặc progress (ở dưới)

                    // Detect lỗi sớm trong vài giây đầu
                    if (!earlyErrorDetected && (System.currentTimeMillis() - startTime) < 3000) {
                        String low = line.toLowerCase();
                        if (low.contains("error opening output")
                                || low.contains("error opening output files")
                                || low.contains("no such file or directory")
                                || low.contains("i/o error")
                                || low.contains("connection refused")
                                || low.contains("failed to resolve hostname")
                                || low.contains("server returned 4")
                                || low.contains("server returned 5")
                                || low.contains("cannot find")
                                || low.contains("invalid argument")) {
                            earlyErrorDetected = true;
                            System.err.println("[FFMPEG] Early error detected for " + streamKey + ": " + line);
                        }
                    }

                    // Parse STATS line (luôn parse để có data mới nhất, nhưng chỉ log/update mỗi
                    // 30s)
                    if (line.contains("frame=") && line.contains("fps=")) {
                        parseStatsLineInto(stat, line);
                        stat.updatedAt = System.currentTimeMillis();
                        // Chỉ update statMap mỗi 30 giây
                        long now = System.currentTimeMillis();
                        if (now - lastLogTime >= LOG_INTERVAL_MS) {
                            statMap.put(streamKey, cloneStat(stat));
                            System.out.println("[FFMPEG] " + streamKey + " | frame=" + stat.frame
                                    + " fps=" + String.format("%.1f", stat.fps)
                                    + " speed=" + stat.speed + " time=" + stat.time);
                            lastLogTime = now;
                        }
                        continue;
                    }

                    // Parse PROGRESS: key=value
                    if (!line.contains("="))
                        continue;

                    String[] kv = line.split("=", 2);
                    if (kv.length != 2)
                        continue;

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
                            // Chỉ update statMap mỗi 30 giây
                            long now = System.currentTimeMillis();
                            if (now - lastLogTime >= LOG_INTERVAL_MS) {
                                statMap.put(streamKey, cloneStat(stat));
                                System.out.println("[FFMPEG] " + streamKey + " | frame=" + stat.frame
                                        + " fps=" + String.format("%.1f", stat.fps != null ? stat.fps : 0.0)
                                        + " speed=" + stat.speed + " time=" + stat.time);
                                lastLogTime = now;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Đọc phần còn lại nếu có (khi process đã chết nhưng stream vẫn còn dữ liệu)
            if (br.ready()) {
                while ((line = br.readLine()) != null) {
                    System.out.println("[FFMPEG] " + line);
                }
            }

        } catch (IOException e) {
            // Chỉ log nếu process vẫn còn sống (có thể là lỗi thực sự)
            if (process.isAlive()) {
                System.err.println("[FFMPEG] IO error reading stream for " + streamKey + ": " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[FFMPEG] Unexpected error reading stream for " + streamKey + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Đóng resources một cách an toàn
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            // Cleanup maps
            processMap.remove(streamKey);
            statMap.remove(streamKey);
        }
    }

    private static void parseStatsLineInto(FfmpegStat stat, String line) {
        try {
            Long frame = extractLongAfter(line, "frame=");
            if (frame != null)
                stat.frame = frame;

            Double fps = extractDoubleAfter(line, "fps=");
            if (fps != null)
                stat.fps = fps;

            Double q = extractDoubleAfter(line, "q=");
            if (q != null)
                stat.q = q;

            String bitrate = extractTokenAfter(line, "bitrate=");
            if (bitrate != null)
                stat.bitrate = bitrate;

            String speed = extractTokenAfter(line, "speed=");
            if (speed != null)
                stat.speed = speed;

            String time = extractTokenAfter(line, "time=");
            if (time != null)
                stat.time = time;

            String size = extractTokenAfter(line, "size=");
            if (size != null)
                stat.size = size;

        } catch (Exception ignored) {
        }
    }

    private static String extractTokenAfter(String line, String key) {
        int i = line.indexOf(key);
        if (i < 0)
            return null;
        String tail = line.substring(i + key.length()).trim();
        if (tail.isEmpty())
            return null;
        int sp = tail.indexOf(' ');
        return (sp < 0) ? tail : tail.substring(0, sp).trim();
    }

    private static Long extractLongAfter(String line, String key) {
        String t = extractTokenAfter(line, key);
        if (t == null)
            return null;
        try {
            return Long.parseLong(t.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double extractDoubleAfter(String line, String key) {
        String t = extractTokenAfter(line, key);
        if (t == null)
            return null;
        try {
            return Double.parseDouble(t.trim());
        } catch (Exception e) {
            return null;
        }
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
        if (bytes < 1024)
            return bytes + "B";
        double kib = bytes / 1024.0;
        if (kib < 1024)
            return String.format(java.util.Locale.US, "%.0fKiB", kib);
        double mib = kib / 1024.0;
        if (mib < 1024)
            return String.format(java.util.Locale.US, "%.1fMiB", mib);
        double gib = mib / 1024.0;
        return String.format(java.util.Locale.US, "%.2fGiB", gib);
    }

    @Override
    public void stopStream(String streamKey) {
        if (streamKey == null || streamKey.isBlank())
            return;

        Process process = processMap.get(streamKey); // Lấy nhưng chưa remove ngay
        if (process == null) {
            // Đã được remove rồi hoặc không tồn tại
            statMap.remove(streamKey);
            return;
        }

        // Remove khỏi map để readProgress biết đã stop
        processMap.remove(streamKey);
        statMap.remove(streamKey);

        if (!process.isAlive()) {
            // Process đã chết rồi
            return;
        }

        try {
            // Gửi lệnh 'q' để FFmpeg dừng gracefully
            OutputStream os = process.getOutputStream();
            if (os != null) {
                try {
                    os.write("q\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    os.flush();
                    os.close(); // Đóng stream sau khi gửi lệnh
                } catch (IOException e) {
                    // Nếu không thể gửi lệnh, sẽ force kill ở dưới
                }
            }

            // Đợi process kết thúc trong 5 giây
            boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
            if (!terminated) {
                // Nếu không kết thúc, force kill
                System.out.println("[FFMPEG] Process not terminated gracefully, forcing kill for " + streamKey);
                process.destroyForcibly();
                // Đợi thêm một chút để đảm bảo process đã chết
                try {
                    process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } catch (Exception e) {
            // Bất kỳ lỗi nào khác, force kill
            System.err.println("[FFMPEG] Error stopping stream " + streamKey + ": " + e.getMessage());
            process.destroyForcibly();
        }

        System.out.println("[FFMPEG] Stopped stream: " + streamKey);
    }

    @Override
    public FfmpegStat getLatestStat(String streamKey) {
        return statMap.get(streamKey);
    }

    private static long parseLong(String v) {
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static double parseDouble(String v) {
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static boolean isUrl(String s) {
        if (s == null)
            return false;
        String x = s.trim().toLowerCase();
        return x.startsWith("http://") || x.startsWith("https://");
    }

    private static boolean isLocalFile(String s) {
        return s != null && !isUrl(s);
    }

    /**
     * Kiểm tra process còn sống sau khi start.
     * KHÔNG đọc từ stream để tránh conflict với readProgress trên Windows.
     * readProgress sẽ tự detect lỗi từ output của FFmpeg.
     */
    private void assertProcessStartedOk(String streamKey, Process process, long waitMs) {
        long end = System.currentTimeMillis() + waitMs;
        long checkInterval = 50; // ms

        try {
            // Chỉ kiểm tra process còn sống, không đọc stream
            while (System.currentTimeMillis() < end) {
                if (!process.isAlive()) {
                    // Process đã chết sớm - lấy exit code nếu có thể
                    try {
                        int code = process.exitValue();
                        throw new RuntimeException("FFMPEG_START_FAILED: Process exited early with code=" + code);
                    } catch (IllegalThreadStateException e) {
                        // Process chưa kết thúc hoàn toàn, nhưng isAlive() trả về false
                        // Có thể là race condition, đợi thêm một chút
                        Thread.sleep(100);
                        if (!process.isAlive()) {
                            int code = process.exitValue();
                            throw new RuntimeException("FFMPEG_START_FAILED: Process exited early with code=" + code);
                        }
                    }
                }
                Thread.sleep(checkInterval);
            }

            // Kiểm tra lại một lần nữa sau khi hết thời gian chờ
            if (!process.isAlive()) {
                try {
                    int code = process.exitValue();
                    throw new RuntimeException("FFMPEG_START_FAILED: Process exited with code=" + code);
                } catch (IllegalThreadStateException e) {
                    // Process vẫn đang chạy (race condition), coi như OK
                }
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Nếu bị interrupt, kiểm tra process một lần nữa
            if (!process.isAlive()) {
                try {
                    int code = process.exitValue();
                    throw new RuntimeException("FFMPEG_START_FAILED: Process exited with code=" + code);
                } catch (IllegalThreadStateException ignored) {
                    // Process vẫn đang chạy, coi như OK
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("FFMPEG_START_CHECK_ERROR: " + e.getMessage(), e);
        }
    }
}
