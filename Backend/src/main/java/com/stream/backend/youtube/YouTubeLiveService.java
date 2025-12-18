package com.stream.backend.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.stream.backend.entity.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YouTubeLiveService {

    private final YouTube youtube;

    /**
     * KHÔNG TẠO LỊCH TRÊN YOUTUBE NỮA.
     * Bạn tạo lịch thủ công trên YouTube Studio và nhập keyStream vào Stream.
     *
     * Giữ method này để tránh vỡ code nơi khác: chỉ validate và trả lại keyStream.
     */
    public String createScheduledBroadcastForStream(Stream stream) {
        // Không gọi YouTube API ở đây nữa
        if (stream.getKeyStream() == null || stream.getKeyStream().isBlank()) {
            throw new RuntimeException("keyStream trống. Hãy tạo lịch trên YouTube Studio và nhập stream key vào hệ thống.");
        }
        return stream.getKeyStream();
    }

    /**
     * Transition broadcast: "live" hoặc "complete".
     * Chỉ gọi được khi stream.youtubeBroadcastId đã có.
     */
    public void transitionBroadcast(Stream stream, String newStatus) throws IOException {
        if (stream == null) return;
        if (stream.getYoutubeBroadcastId() == null || stream.getYoutubeBroadcastId().isBlank()) return;

        youtube.liveBroadcasts()
                .transition(newStatus, stream.getYoutubeBroadcastId(), "status")
                .execute();
    }

    /**
     * (Tuỳ chọn) Helper: nếu bạn muốn tự "tìm broadcast" do bạn tạo thủ công
     * theo title (không 100% chính xác nếu trùng tên), có thể dùng để set youtubeBroadcastId.
     * Không bắt buộc dùng trong demo.
     */
    public String findLatestBroadcastIdByTitle(String title) throws IOException {
        if (title == null || title.isBlank()) return null;

        LiveBroadcastListResponse resp = youtube.liveBroadcasts()
                .list("id,snippet,status")
                .setBroadcastStatus("upcoming") // upcoming / active / completed
                .setMine(true)
                .setMaxResults(10L)
                .execute();

        List<LiveBroadcast> items = resp.getItems();
        if (items == null) return null;

        for (LiveBroadcast b : items) {
            if (b.getSnippet() != null && title.equalsIgnoreCase(b.getSnippet().getTitle())) {
                return b.getId();
            }
        }
        return null;
    }
}
