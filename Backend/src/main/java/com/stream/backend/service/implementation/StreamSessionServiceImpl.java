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
import org.springframework.data.jpa.domain.JpaSort;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

            if ("stream.name".equals(field)) {
                // Natural sort: Order by LENGTH(name), then name
                return JpaSort.unsafe(dir, "LENGTH(s.name)", "s.name");
            }

            return Sort.by(dir, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
    }

    private boolean isAllowedSortField(String field) {
        return "id".equals(field)
                || "status".equals(field)
                || "startedAt".equals(field)
                || "stoppedAt".equals(field)
                || "stream.name".equals(field);
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

    @Override
    public Page<StreamSession> adminGetAll(String status, String ownerName, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        String s = (status == null || status.isBlank()) ? null : status.trim();
        String o = (ownerName == null || ownerName.isBlank()) ? null : ownerName.trim();
        return streamSessionRepository.findAllByOptionalStatusAndOwnerName(s, o, pageable);
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

    @Override
    public List<StreamSession> getAllStreamSessionsList(Integer userId, String sort) {
        Sort sortObj = parseSort(sort);
        return streamSessionRepository.findActiveOrScheduledByUserIdList(userId, sortObj);
    }

    @Override
    @Transactional
    public void deleteVideoAndResetStream(StreamSession session) {
        if (session == null || session.getId() == null) {
            log.warn("[DELETE-VIDEO] Session is null or has no ID");
            return;
        }

        // Reload entities within transaction to ensure they're managed
        StreamSession managedSession = streamSessionRepository.findById(session.getId())
                .orElse(null);
        if (managedSession == null) {
            log.warn("[DELETE-VIDEO] Session not found: sessionId={}", session.getId());
            return;
        }

        Stream stream = managedSession.getStream();
        if (stream == null || stream.getId() == null) {
            log.warn("[DELETE-VIDEO] Stream is null or has no ID for sessionId={}", managedSession.getId());
            return;
        }

        // Reload stream to ensure it's managed
        Stream managedStream = streamRepository.findById(stream.getId())
                .orElse(null);
        if (managedStream == null) {
            log.warn("[DELETE-VIDEO] Stream not found: streamId={}", stream.getId());
            return;
        }

        String videoList = managedStream.getVideoList();
        if (videoList == null || videoList.isBlank()) {
            log.info("[DELETE-VIDEO] No video path for streamId={}, sessionId={}", managedStream.getId(),
                    managedSession.getId());
        } else {
            // Xóa file video dựa vào đường dẫn
            String[] videoPaths = videoList.split("\\r?\\n");
            for (String videoPath : videoPaths) {
                videoPath = videoPath.trim();
                if (videoPath.isEmpty())
                    continue;

                // Chỉ xóa file local (không phải URL)
                if (isLocalFile(videoPath)) {
                    try {
                        File videoFile = new File(videoPath);
                        if (videoFile.exists()) {
                            boolean deleted = videoFile.delete();
                            if (deleted) {
                                log.info("[DELETE-VIDEO] Deleted video file: {}", videoPath);
                            } else {
                                log.warn("[DELETE-VIDEO] Failed to delete video file: {}", videoPath);
                            }
                        } else {
                            log.info("[DELETE-VIDEO] Video file not found (may already deleted): {}", videoPath);
                        }
                    } catch (Exception e) {
                        log.error("[DELETE-VIDEO] Error deleting video file: {}", videoPath, e);
                    }
                } else {
                    log.info("[DELETE-VIDEO] Skipping URL (not local file): {}", videoPath);
                }
            }
        }

        // Lưu streamId và sessionId trước khi xóa để log
        Integer streamId = managedStream.getId();
        Integer sessionId = managedSession.getId();

        // QUAN TRỌNG: Set streamSession thành null TRƯỚC KHI xóa để tránh lỗi
        // TransientObjectException
        // Vì Stream có @OneToOne(mappedBy) với StreamSession, cần xóa reference trước
        managedStream.setStreamSession(null);
        streamRepository.save(managedStream);
        streamRepository.flush(); // Flush để đảm bảo reference được xóa trong DB

        // Xóa StreamSession bằng ID sau khi đã xóa reference
        streamSessionRepository.deleteById(sessionId);

        // Xóa videoList, timeStart và duration trong Stream để stream trở về trạng thái
        // NONE hoàn toàn
        // User có thể lên lịch scheduled lại từ đầu
        managedStream.setVideoList(null);
        managedStream.setTimeStart(null);
        managedStream.setDuration(null);

        streamRepository.save(managedStream);

        log.info("[DELETE-VIDEO] Reset stream to NONE state: streamId={}, sessionId={}", streamId, sessionId);
    }

    private boolean isLocalFile(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        // Kiểm tra nếu là đường dẫn Windows (C:\...) hoặc Unix (/...)
        // và không phải là URL (http://, https://)
        return !path.startsWith("http://")
                && !path.startsWith("https://")
                && (path.matches("^[a-zA-Z]:\\\\.*") || path.startsWith("/"));
    }

    @Override
    public Map<String, Object> getAdminStats() {
        long totalStreams = streamRepository.count();
        long scheduled = streamSessionRepository.countByStatusIgnoreCase("SCHEDULED");
        long active = streamSessionRepository.countByStatusIgnoreCase("ACTIVE");
        long none = totalStreams - scheduled - active;
        if (none < 0)
            none = 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalStreams);
        stats.put("scheduled", scheduled);
        stats.put("active", active);
        stats.put("none", none);
        return stats;
    }

    @Override
    public List<String> getOwnersByStatus(String status) {
        String s = (status == null || status.isBlank()) ? null : status.trim();
        List<String> owners = streamSessionRepository.findDistinctOwnerNamesByStatus(s);
        owners.sort(String.CASE_INSENSITIVE_ORDER);
        return owners;
    }
}
