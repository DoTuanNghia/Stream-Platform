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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

    // ==========================
    // QUERY
    // ==========================

    @Override
    public List<StreamSession> getAllStreamSessions() {
        return streamSessionRepository.findAll();
    }

    @Override
    public List<StreamSession> getStreamSessionsByDeviceId(Integer deviceId) {
        return streamSessionRepository.findByDeviceId(deviceId);
    }

    @Override
    public List<StreamSession> getStreamSessionsByStreamId(Integer streamId) {
        return streamSessionRepository.findByStreamId(streamId);
    }

    @Override
    public StreamSession getStreamSessionById(Integer streamSessionId) {
        return streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found with id = " + streamSessionId));
    }

    // ==========================
    // STOP STREAM
    // ==========================

    @Override
    @Transactional
    public StreamSession stopStreamSession(StreamSession session) {

        if (session == null) {
            throw new RuntimeException("StreamSession is null");
        }

        String prevStatus = session.getStatus();
        Stream stream = session.getStream();

        // 1. Stop FFmpeg
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

        // 2. Transition YouTube -> complete (BEST EFFORT)
        try {
            if (stream != null) {
                youTubeLiveService.transitionBroadcast(stream, "complete");
            }
        } catch (Exception ex) {
            System.err.println("[STOP] transition complete failed (ignored): " + ex.getMessage());
        }

        // 3. Update session status
        session.setStatus("STOPPED");
        session.setStoppedAt(LocalDateTime.now());
        streamSessionRepository.save(session);

        // 4. Decrease device.currentSession ONLY if was ACTIVE
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

    // ==========================
    // START STREAM (MANUAL)
    // ==========================

    @Override
    @Transactional
    public StreamSession startSessionForStream(Integer streamId) {

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream không tồn tại"));

        List<StreamSession> list = streamSessionRepository.findByStreamId(streamId);
        StreamSession session = list.isEmpty() ? null : list.get(0);

        if (session != null && "ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Stream đang ACTIVE, không thể Stream Ngay.");
        }

        Device device;
        if (session != null && session.getDevice() != null) {
            device = session.getDevice();
        } else {
            List<Device> devices = deviceRepository.findAvailableDevices();
            if (devices.isEmpty()) {
                throw new RuntimeException("Không còn device nào trống");
            }
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
            if (streamKey == null || streamKey.isBlank()) {
                throw new RuntimeException("Stream key trống");
            }

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

    // ==========================
    // START STREAM (SCHEDULED)
    // ==========================

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
        if (streamKey == null || streamKey.isBlank()) {
            throw new RuntimeException("Stream key trống");
        }

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

    // ==========================
    // UTILS
    // ==========================

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
    public StreamSession createStreamSession(StreamSession requestBody, Integer deviceId, Integer streamId) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found with id = " + deviceId));

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream not found with id = " + streamId));

        // 1 Stream chỉ có 1 StreamSession (stream_id UNIQUE)
        List<StreamSession> existing = streamSessionRepository.findByStreamId(streamId);
        if (!existing.isEmpty()) {
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
