package com.stream.backend.service;

public interface FfmpegService {

    /**
     * Bắt đầu stream 1 file video lên RTMP (YouTube).
     *
     * @param videoPath đường dẫn file video
     * @param rtmpUrl   base RTMP (vd: rtmp://a.rtmp.youtube.com/live2)
     * @param streamKey stream key của YouTube
     */
    void startStream(String videoPath, String rtmpUrl, String streamKey);
}
