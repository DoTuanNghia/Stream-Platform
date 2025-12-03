package com.stream.backend.service.implementation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stream.backend.entity.Device;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.DeviceRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.StreamSessionService;

@Service
public class StreamSessionServiceImpl implements StreamSessionService {
    private final StreamSessionRepository streamSessionRepository;
    private final StreamRepository streamRepository;
    private final DeviceRepository deviceRepository;

    public StreamSessionServiceImpl(StreamSessionRepository streamSessionRepository, StreamRepository streamRepository,
            DeviceRepository deviceRepository) {
        this.streamSessionRepository = streamSessionRepository;
        this.streamRepository = streamRepository;
        this.deviceRepository = deviceRepository;
    }

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
    public StreamSession creatStreamSession(StreamSession requestBody,
            Integer deviceId,
            Integer streamId) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found with id = " + deviceId));

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream not found with id = " + streamId));

        // Đảm bảo 1 Stream chỉ có 1 StreamSession (quan hệ 1-1)
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

    @Override
    public void deleteStreamSession(StreamSession streamSession) {
        streamSessionRepository.delete(streamSession);
    }

    @Override
    public StreamSession getStreamSessionById(Integer streamSessionId) {
        return streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found with id = " + streamSessionId));
    }

    @Override
    @Transactional
    public StreamSession stopStreamSession(StreamSession session) {

        // 1. Đổi trạng thái
        session.setStatus("STOPPED");
        streamSessionRepository.save(session);

        // 2. Giảm currentSession của device
        Device device = session.getDevice();
        if (device != null) {
            int current = device.getCurrentSession() != null
                    ? device.getCurrentSession()
                    : 0;
            if (current > 0) {
                device.setCurrentSession(current - 1);
                deviceRepository.save(device);
            }
        }

        return session;
    }

    @Override
    @Transactional
    public StreamSession startSessionForStream(Integer streamId) {

        // 1. Lấy stream từ repo
        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new RuntimeException("Stream không tồn tại"));

        // 2. Tìm device rảnh (ID nhỏ nhất)
        List<Device> devices = deviceRepository.findAvailableDevices();
        if (devices.isEmpty()) {
            throw new RuntimeException("Không còn device nào trống");
        }

        Device device = devices.get(0); // ưu tiên ID nhỏ nhất

        // 3. Tạo session mới
        StreamSession session = new StreamSession();
        session.setStream(stream);
        session.setDevice(device);
        session.setStatus("ACTIVE");
        session.setSpecification("Blank");
        // session.setTimeStart(LocalDateTime.now());

        StreamSession saved = streamSessionRepository.save(session);

        // 4. Tăng currentSession
        device.setCurrentSession(device.getCurrentSession() + 1);
        deviceRepository.save(device);

        return saved;
    }
}
