package com.stream.backend.service;

public interface FfmpegService {

    /**
     * Bắt đầu stream 1 nguồn video lên RTMP (YouTube).
     *
     * @param videoPath đường dẫn file video HOẶC URL (Google Drive, HTTP, ...)
     * @param rtmpUrl   base RTMP (vd: rtmp://x.rtmp.youtube.com/live2)
     * @param streamKey stream key của YouTube
     */
    void startStream(String videoPath, String rtmpUrl, String streamKey);
}
