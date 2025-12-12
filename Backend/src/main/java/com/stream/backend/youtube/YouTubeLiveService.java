package com.stream.backend.youtube;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.stream.backend.entity.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class YouTubeLiveService {

    private final YouTube youtube;

    /**
     * Tạo LiveStream + LiveBroadcast (upcoming) + bind lại với nhau.
     * Trả về streamKey (RTMP stream name).
     */
    public String createScheduledBroadcastForStream(Stream stream) throws IOException {

        // =====================================================
        // 1. Tạo LiveStream (RTMP Ingestion)
        // =====================================================
        LiveStream liveStream = new LiveStream();

        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
        streamSnippet.setTitle(stream.getName() + " - Stream");
        liveStream.setSnippet(streamSnippet);

        CdnSettings cdn = new CdnSettings();
        cdn.setFormat("1080p");
        cdn.setResolution("1080p");
        cdn.setFrameRate("30fps");
        cdn.setIngestionType("rtmp");
        liveStream.setCdn(cdn);

        LiveStreamStatus streamStatus = new LiveStreamStatus();
        streamStatus.setStreamStatus("ready");  
        liveStream.setStatus(streamStatus);

        LiveStream createdStream = youtube.liveStreams()
                .insert("snippet,cdn,status", liveStream)
                .execute();

        String youtubeStreamId = createdStream.getId();
        String streamKey = createdStream.getCdn().getIngestionInfo().getStreamName();

        // =====================================================
        // 2. Tạo LiveBroadcast (Upcoming)
        // =====================================================
        LiveBroadcast broadcast = new LiveBroadcast();

        LiveBroadcastSnippet snippet = new LiveBroadcastSnippet();
        snippet.setTitle(stream.getName());
        snippet.setDescription("Scheduled via Stream Platform");

        // set thời gian bắt đầu
        if (stream.getTimeStart() != null) {
            snippet.setScheduledStartTime(toDateTime(stream.getTimeStart()));
        }

        // set thời gian kết thúc nếu có duration
        if (stream.getTimeStart() != null && stream.getDuration() != null && stream.getDuration() > 0) {
            LocalDateTime end = stream.getTimeStart().plusMinutes(stream.getDuration());
            snippet.setScheduledEndTime(toDateTime(end));
        }

        broadcast.setSnippet(snippet);

        // Status: public / unlisted / private
        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("public");
        broadcast.setStatus(status);

        // =====================================================
        // FIX QUAN TRỌNG – TRÁNH LỖI invalidEmbedSetting
        // =====================================================
        LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();

        contentDetails.setEnableDvr(true);
        contentDetails.setRecordFromStart(true);

        // Một số version có hỗ trợ AutoStart
        try {
            contentDetails.setEnableAutoStart(true);
        } catch (Exception ignored) {
            // nếu version không hỗ trợ thì bỏ qua
        }

        broadcast.setContentDetails(contentDetails);

        // =====================================================
        // 3. Tạo Broadcast trên YouTube
        // =====================================================
        LiveBroadcast createdBroadcast = youtube.liveBroadcasts()
                .insert("snippet,contentDetails,status", broadcast)
                .execute();

        String youtubeBroadcastId = createdBroadcast.getId();

        // =====================================================
        // 4. Bind stream ↔ broadcast
        // =====================================================
        youtube.liveBroadcasts()
                .bind(youtubeBroadcastId, "id,contentDetails")
                .setStreamId(youtubeStreamId)
                .execute();

        // Lưu vào entity Stream
        stream.setYoutubeBroadcastId(youtubeBroadcastId);
        stream.setYoutubeStreamId(youtubeStreamId);

        return streamKey;
    }

    /**
     * Chuyển trạng thái broadcast: "live" hoặc "complete"
     */
    public void transitionBroadcast(Stream stream, String newStatus) throws IOException {
        if (stream.getYoutubeBroadcastId() == null) return;

        youtube.liveBroadcasts()
                .transition(newStatus, stream.getYoutubeBroadcastId(), "status")
                .execute();
    }

    private DateTime toDateTime(LocalDateTime ldt) {
        return new DateTime(ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
