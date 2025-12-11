package com.stream.backend.service.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stream.backend.service.FfmpegService;

@Service
public class FfmpegServiceImpl implements FfmpegService {

    @Value("${stream.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${stream.youtube.rtmp}")
    private String youtubeRtmp;

    @Value("${stream.demo.video}")
    private String demoVideoPath;

    // Lưu process FFmpeg theo streamKey để sau còn stop được
    private final Map<String, Process> processMap = new ConcurrentHashMap<>();

    @Override
    public void startStream(String videoPath, String rtmpUrl, String streamKey) {
        // Nếu không truyền videoPath (hoặc rỗng) thì dùng video demo trong config
        if (videoPath == null || videoPath.isBlank()) {
            videoPath = demoVideoPath;
        }

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
            cmd.add("-re");
            // loop vô hạn video
            cmd.add("-stream_loop");
            cmd.add("-1");

            cmd.add("-i");
            cmd.add(videoPath);
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("veryfast");
            cmd.add("-maxrate");
            cmd.add("3000k");
            cmd.add("-bufsize");
            cmd.add("6000k");
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("128k");
            cmd.add("-ar");
            cmd.add("44100");
            cmd.add("-f");
            cmd.add("flv");
            cmd.add(fullRtmp);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // gộp stderr vào stdout
            Process process = pb.start();

            // LƯU process vào map để sau này stop được
            processMap.put(keyForMap, process);

            // In log ffmpeg ra console
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFMPEG] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Khi process tự kết thúc (video hết / lỗi) thì xoá khỏi map
                    processMap.remove(keyForMap);
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

}
