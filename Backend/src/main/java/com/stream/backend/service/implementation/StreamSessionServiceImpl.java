package com.stream.backend.service.implementation;

import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.FfmpegService;
import com.stream.backend.service.StreamSessionService;
import com.stream.backend.youtube.YouTubeLiveService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StreamSessionServiceImpl implements StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamRepository streamRepository;
    private final FfmpegService ffmpegService;
    private final YouTubeLiveService youTubeLiveService;

    public StreamSessionServiceImpl(
            StreamSessionRepository streamSessionRepository,
            StreamRepository streamRepository,
            FfmpegService ffmpegService,
            YouTubeLiveService youTubeLiveService) {

        this.streamSessionRepository = streamSessionRepository;
        this.streamRepository = streamRepository;
        this.ffmpegService = ffmpegService;
        this.youTubeLiveService = youTubeLiveService;
    }

    @Override
    public Page<StreamSession> getAllStreamSessions(Integer userId, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return streamSessionRepository.findActiveOrScheduledByUserId(userId, pageable);
    }

    @Override
    public Page<StreamSession> getStreamSessionsByStreamId(Integer streamId, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return streamSessionRepository.findByStreamId(streamId, pageable);
    }

    @Override
    public Map<Integer, String> getStatusMapByStreamIds(List<Integer> streamIds) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (streamIds == null || streamIds.isEmpty())
            return map;

        for (Integer streamId : streamIds) {
            if (streamId == null)
                continue;

            StreamSession ss = streamSessionRepository
                    .findTopByStreamIdOrderByIdDesc(streamId)
                    .orElse(null);

            if (ss != null && ss.getStatus() != null) {
                map.put(streamId, ss.getStatus().toUpperCase());
            }
        }
        return map;
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Sort sortObj = parseSort(sort);
        return PageRequest.of(safePage, safeSize, sortObj);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;

            if (!isAllowedSortField(field))
                field = "id";
            return Sort.by(dir, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
    }

    private boolean isAllowedSortField(String field) {
        return "id".equals(field)
                || "status".equals(field)
                || "startedAt".equals(field)
                || "stoppedAt".equals(field);
    }

    @Override
    public StreamSession getStreamSessionById(Integer streamSessionId) {
        return streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found with id = " + streamSessionId));
    }

    @Override
    @Transactional
    public StreamSession stopStreamSession(StreamSession session) {

        if (session == null)
            throw new RuntimeException("StreamSession is null");

        Stream stream = session.getStream();

        try {
            if (stream != null) {
                String streamKey = stream.getKeyStream();
                System.out.println("[STOP] Stopping FFmpeg with streamKey = " + streamKey);
                ffmpegService.stopStream(streamKey);
            }
        } catch (Exception e) {
            System.err.println("[STOP] Cannot stop FFmpeg for sessionId=" + session.getId());
            e.printStackTrace();
        }

        try {
            if (stream != null) {
                youTubeLiveService.transitionBroadcast(stream, "complete");
            }
        } catch (Exception ex) {
            System.err.println("[STOP] transition complete failed (ignored): " + ex.getMessage());
        }

        session.setStatus("STOPPED");
        session.setStoppedAt(LocalDateTime.now());
        streamSessionRepository.save(session);

        return session;
    }

    @Override
    @Transactional
    public StreamSession startSessionForStream(Integer streamId) {
        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream không tồn tại"));

        StreamSession session = streamSessionRepository
                .findTopByStreamIdOrderByIdDesc(streamId)
                .orElse(null);

        if (session != null && "ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Stream đang ACTIVE, không thể Stream Ngay.");
        }

        if (session == null) {
            session = new StreamSession();
            session.setStream(stream);
        }

        session.setSpecification("Manual start");
        session.setStartedAt(null);
        session.setStoppedAt(null);
        session.setLastError(null);
        session.setLastErrorAt(null);
        streamSessionRepository.save(session);

        String streamKey = stream.getKeyStream();
        if (streamKey == null || streamKey.isBlank()) {
            markError(session, "STREAM_KEY_EMPTY");
            return session;
        }

        String videoSource = resolveFirstVideoSource(stream);

        try {
            ffmpegService.startStream(videoSource, null, streamKey);

            session.setStatus("ACTIVE");
            session.setStartedAt(LocalDateTime.now());
            session.setStoppedAt(null);
            streamSessionRepository.save(session);

            return session;

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FFMPEG_START_FAILED";
            markError(session, msg);
            throw new RuntimeException("Không thể bắt đầu stream: " + msg);
        }
    }

    @Override
    @Transactional
    public StreamSession startScheduledSession(Integer streamSessionId) {
        StreamSession session = streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found"));

        if (!"SCHEDULED".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Session is not SCHEDULED");
        }

        Stream stream = session.getStream();

        session.setSpecification("Auto start");
        session.setLastError(null);
        session.setLastErrorAt(null);
        streamSessionRepository.save(session);

        String streamKey = stream.getKeyStream();
        if (streamKey == null || streamKey.isBlank()) {
            markError(session, "STREAM_KEY_EMPTY");
            throw new RuntimeException("Stream key trống");
        }

        String videoSource = resolveFirstVideoSource(stream);

        try {
            ffmpegService.startStream(videoSource, null, streamKey);

            session.setStatus("ACTIVE");
            session.setStartedAt(LocalDateTime.now());
            session.setStoppedAt(null);
            streamSessionRepository.save(session);

            return session;

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "FFMPEG_START_FAILED";
            markError(session, msg);

            // scheduler không cần throw, để transaction commit và lưu ERROR
            return session;
        }

    }

    private String normalizeVideoSource(String raw) {
        if (raw == null)
            return null;
        raw = raw.trim();

        Pattern p = Pattern.compile("https?://drive\\.google\\.com/file/d/([^/]+)/view.*");
        Matcher m = p.matcher(raw);
        if (m.matches()) {
            return "https://drive.google.com/uc?export=download&id=" + m.group(1);
        }
        return raw;
    }

    @Override
    public Page<StreamSession> adminGetAll(String status, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        String s = (status == null || status.isBlank()) ? null : status.trim();
        return streamSessionRepository.findAllByOptionalStatus(s, pageable);
    }

    private void markError(StreamSession session, String msg) {
        session.setStatus("ERROR");
        session.setLastError(msg);
        session.setLastErrorAt(LocalDateTime.now());
        streamSessionRepository.save(session);
    }

    private String resolveFirstVideoSource(Stream stream) {
        String videoSource = null;
        if (stream.getVideoList() != null && !stream.getVideoList().isBlank()) {
            videoSource = Arrays.stream(stream.getVideoList().split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::normalizeVideoSource)
                    .findFirst()
                    .orElse(null);
        }
        return videoSource;
    }
}
