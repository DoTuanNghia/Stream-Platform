package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stream.backend.entity.Channel;
import com.stream.backend.entity.Device;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.repository.DeviceRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.StreamService;

@Service
public class StreamServiceImpl implements StreamService {
    private final StreamRepository streamRepository;
    private final ChannelRepository channelRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final DeviceRepository deviceRepository;

    public StreamServiceImpl(StreamRepository streamRepository, ChannelRepository channelRepository, StreamSessionRepository streamSessionRepository, DeviceRepository deviceRepository){ 
        this.streamRepository = streamRepository;
        this.channelRepository = channelRepository;
        this.streamSessionRepository = streamSessionRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public List<Stream> getStreamsByChannelId(Integer channelId) {
        return streamRepository.findByChannelId(channelId);
    }

    @Override
    public Stream createStream(Stream stream, Integer channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElse(null);
        stream.setChannel(channel);
        return streamRepository.save(stream);
    }

    @Override
    @Transactional
    public void deleteStream(Stream stream) {

        // Lấy thật stream từ DB
        Stream existingStream = streamRepository.findById(stream.getId())
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        // Lấy tất cả session liên quan
        List<StreamSession> sessions = streamSessionRepository.findByStreamId(existingStream.getId());

        for (StreamSession session : sessions) {

            // giảm currentSession nếu session đang ACTIVE
            if (!"STOPPED".equalsIgnoreCase(session.getStatus())) {
                Device device = session.getDevice();
                if (device != null) {
                    int current = device.getCurrentSession() == null ? 0 : device.getCurrentSession();
                    device.setCurrentSession(Math.max(0, current - 1));
                    deviceRepository.save(device);
                }
            }

            // xoá session
            streamSessionRepository.delete(session);
        }

        // cuối cùng xoá stream
        streamRepository.delete(existingStream);
    }

}
