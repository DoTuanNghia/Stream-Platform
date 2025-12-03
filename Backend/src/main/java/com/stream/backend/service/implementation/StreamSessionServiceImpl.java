package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

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
    public StreamSession stopStreamSession(StreamSession streamSession) {
        streamSession.setStatus("STOPPED");
        return streamSessionRepository.save(streamSession);
    }

    @Override
    public StreamSession getStreamSessionById(Integer streamSessionId) {
        return streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found with id = " + streamSessionId));
    }
}
