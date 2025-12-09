package com.stream.backend.service.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void startStream(String videoPath, String rtmpUrl, String streamKey) {
        // Nếu không truyền videoPath thì dùng video demo trong config
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

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-re");
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

            // In log ffmpeg ra console (không bắt buộc, nhưng hữu ích khi debug)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFMPEG] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            System.out.println("Started FFmpeg process for stream key: " + streamKey);

        } catch (IOException e) {
            throw new RuntimeException("Failed to start FFmpeg process", e);
        }
    }
}
