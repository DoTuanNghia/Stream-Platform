package com.stream.backend.service.implementation;

import com.stream.backend.entity.Device;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.DeviceRepository;
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
    private final DeviceRepository deviceRepository;
    private final FfmpegService ffmpegService;
    private final YouTubeLiveService youTubeLiveService;

    public StreamSessionServiceImpl(
            StreamSessionRepository streamSessionRepository,
            StreamRepository streamRepository,
            DeviceRepository deviceRepository,
            FfmpegService ffmpegService,
            YouTubeLiveService youTubeLiveService) {

        this.streamSessionRepository = streamSessionRepository;
        this.streamRepository = streamRepository;
        this.deviceRepository = deviceRepository;
        this.ffmpegService = ffmpegService;
        this.youTubeLiveService = youTubeLiveService;
    }

    @Override
    public Page<StreamSession> getAllStreamSessions(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        // CHỈ ACTIVE để hiển thị + phân trang trang StreamSession
        return streamSessionRepository.findByStatusIgnoreCase("ACTIVE", pageable);
    }

    @Override
    public Page<StreamSession> getStreamSessionsByDeviceId(Integer deviceId, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        // Nếu bạn cũng muốn device chỉ ACTIVE thì đổi sang query theo status + device, còn hiện tại giữ nguyên
        return streamSessionRepository.findByDeviceId(deviceId, pageable);
    }

    @Override
    public Page<StreamSession> getStreamSessionsByStreamId(Integer streamId, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return streamSessionRepository.findByStreamId(streamId, pageable);
    }

    @Override
    public Map<Integer, String> getStatusMapByStreamIds(List<Integer> streamIds) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (streamIds == null || streamIds.isEmpty()) return map;

        for (Integer streamId : streamIds) {
            if (streamId == null) continue;
            StreamSession ss = streamSessionRepository.findFirstByStreamId(streamId).orElse(null);
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

            if (!isAllowedSortField(field)) field = "id";
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

        if (session == null) throw new RuntimeException("StreamSession is null");

        String prevStatus = session.getStatus();
        Stream stream = session.getStream();

        // 1) Stop FFmpeg
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

        // 2) Transition YouTube -> complete (best effort)
        try {
            if (stream != null) youTubeLiveService.transitionBroadcast(stream, "complete");
        } catch (Exception ex) {
            System.err.println("[STOP] transition complete failed (ignored): " + ex.getMessage());
        }

        // 3) Update status STOPPED
        session.setStatus("STOPPED");
        session.setStoppedAt(LocalDateTime.now());
        streamSessionRepository.save(session);

        // 4) Decrease device.currentSession ONLY if was ACTIVE
        if ("ACTIVE".equalsIgnoreCase(prevStatus)) {
            Device device = session.getDevice();
            if (device != null) {
                int current = device.getCurrentSession() != null ? device.getCurrentSession() : 0;
                if (current > 0) {
                    device.setCurrentSession(current - 1);
                    deviceRepository.save(device);
                }
            }
        }
        return session;
    }

    @Override
    @Transactional
    public StreamSession startSessionForStream(Integer streamId) {

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream không tồn tại"));

        StreamSession session = streamSessionRepository.findFirstByStreamId(streamId).orElse(null);

        // chặn ACTIVE (STOPPED vẫn chặn ở FE theo yêu cầu)
        if (session != null && "ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Stream đang ACTIVE, không thể Stream Ngay.");
        }

        Device device;
        if (session != null && session.getDevice() != null) {
            device = session.getDevice();
        } else {
            List<Device> devices = deviceRepository.findAvailableDevices();
            if (devices.isEmpty()) throw new RuntimeException("Không còn device nào trống");
            device = devices.get(0);
        }

        if (session == null) {
            session = new StreamSession();
            session.setStream(stream);
            session.setDevice(device);
            session.setSpecification("Manual start");
        } else {
            session.setDevice(device);
            session.setSpecification("Manual start (override schedule)");
        }

        session.setStatus("ACTIVE");
        session.setStartedAt(LocalDateTime.now());
        session.setStoppedAt(null);
        streamSessionRepository.save(session);

        Integer cur = device.getCurrentSession() == null ? 0 : device.getCurrentSession();
        device.setCurrentSession(cur + 1);
        deviceRepository.save(device);

        try {
            String streamKey = stream.getKeyStream();
            if (streamKey == null || streamKey.isBlank()) throw new RuntimeException("Stream key trống");

            String videoSource = null;
            if (stream.getVideoList() != null && !stream.getVideoList().isBlank()) {
                videoSource = Arrays.stream(stream.getVideoList().split("\\r?\\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(this::normalizeVideoSource)
                        .findFirst()
                        .orElse(null);
            }

            ffmpegService.startStream(videoSource, null, streamKey);
        } catch (Exception e) {
            System.err.println("[START] Cannot start FFmpeg for streamId=" + streamId);
            e.printStackTrace();
        }

        return session;
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
        Device device = session.getDevice();

        session.setStatus("ACTIVE");
        session.setStartedAt(LocalDateTime.now());
        session.setStoppedAt(null);
        streamSessionRepository.save(session);

        Integer cur = device.getCurrentSession() == null ? 0 : device.getCurrentSession();
        device.setCurrentSession(cur + 1);
        deviceRepository.save(device);

        String streamKey = stream.getKeyStream();
        if (streamKey == null || streamKey.isBlank()) throw new RuntimeException("Stream key trống");

        String videoSource = null;
        if (stream.getVideoList() != null && !stream.getVideoList().isBlank()) {
            videoSource = Arrays.stream(stream.getVideoList().split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::normalizeVideoSource)
                    .findFirst()
                    .orElse(null);
        }

        ffmpegService.startStream(videoSource, null, streamKey);
        return session;
    }

    private String normalizeVideoSource(String raw) {
        if (raw == null) return null;
        raw = raw.trim();

        Pattern p = Pattern.compile("https?://drive\\.google\\.com/file/d/([^/]+)/view.*");
        Matcher m = p.matcher(raw);
        if (m.matches()) {
            return "https://drive.google.com/uc?export=download&id=" + m.group(1);
        }
        return raw;
    }

    @Override
    public StreamSession createStreamSession(StreamSession requestBody, Integer deviceId, Integer streamId) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found with id = " + deviceId));

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream not found with id = " + streamId));

        if (streamSessionRepository.existsByStreamId(streamId)) {
            throw new RuntimeException("This stream already has a StreamSession");
        }

        StreamSession session = StreamSession.builder()
                .specification(requestBody.getSpecification())
                .status(requestBody.getStatus())
                .device(device)
                .stream(stream)
                .build();

        return streamSessionRepository.save(session);
    }
}
