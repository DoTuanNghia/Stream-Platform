package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.stream.backend.entity.Channel;
import com.stream.backend.entity.Device;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.repository.DeviceRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.FfmpegService;
import com.stream.backend.service.StreamService;
import com.stream.backend.youtube.YouTubeLiveService;

@Service
public class StreamServiceImpl implements StreamService {
    private final StreamRepository streamRepository;
    private final ChannelRepository channelRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final YouTubeLiveService youTubeLiveService;
    private final FfmpegService ffmpegService;

    public StreamServiceImpl(StreamRepository streamRepository,
            ChannelRepository channelRepository,
            StreamSessionRepository streamSessionRepository,
            DeviceRepository deviceRepository,
            YouTubeLiveService youTubeLiveService,
            FfmpegService ffmpegService) {

        this.streamRepository = streamRepository;
        this.channelRepository = channelRepository;
        this.streamSessionRepository = streamSessionRepository;
        this.deviceRepository = deviceRepository;
        this.youTubeLiveService = youTubeLiveService;
        this.ffmpegService = ffmpegService;
    }

    @Override
    public List<Stream> getStreamsByChannelId(Integer channelId) {
        return streamRepository.findByChannelId(channelId);
    }

    @Override
    @Transactional
    public Stream createStream(Stream stream, Integer channelId) {

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        stream.setChannel(channel);

        // 1. Tạo lịch + streamKey trên YouTube
        try {
            String streamKey = youTubeLiveService.createScheduledBroadcastForStream(stream);
            stream.setKeyStream(streamKey);
        } catch (Exception e) {
            throw new RuntimeException("Không tạo được lịch YouTube", e);
        }

        // 2. Lưu Stream
        Stream savedStream = streamRepository.save(stream);

        // 3. AUTO TẠO StreamSession SCHEDULED (nếu có timeStart & autoStart)
        if (Boolean.TRUE.equals(savedStream.getAutoStart())
                && savedStream.getTimeStart() != null
                && savedStream.getTimeStart().isAfter(java.time.LocalDateTime.now())) {

            // tìm device rảnh
            List<Device> devices = deviceRepository.findAvailableDevices();
            if (devices.isEmpty()) {
                throw new RuntimeException("Không còn device trống để schedule");
            }

            Device device = devices.get(0);

            StreamSession session = StreamSession.builder()
                    .stream(savedStream)
                    .device(device)
                    .status("SCHEDULED")
                    .specification("Auto scheduled")
                    .build();

            streamSessionRepository.save(session);
        }

        return savedStream;
    }

    @Override
    @Transactional
    public void deleteStream(Stream stream) {

        Stream existingStream = streamRepository.findById(stream.getId())
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        // Lấy tất cả session theo streamId bằng paging vòng lặp (không dùng List)
        int page = 0;
        int size = 200;
        Page<StreamSession> sessionsPage;

        do {
            sessionsPage = streamSessionRepository.findByStreamId(
                    existingStream.getId(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));

            for (StreamSession session : sessionsPage.getContent()) {

                // 1) Nếu ACTIVE -> stop ffmpeg + transition youtube
                if ("ACTIVE".equalsIgnoreCase(session.getStatus())) {

                    String streamKey = existingStream.getKeyStream();
                    if (streamKey != null && !streamKey.isBlank()) {
                        ffmpegService.stopStream(streamKey);
                    }

                    try {
                        youTubeLiveService.transitionBroadcast(existingStream, "complete");
                    } catch (Exception ignore) {
                    }
                }

                // 2) Giảm currentSession của device
                Device device = session.getDevice();
                if (device != null) {
                    int current = device.getCurrentSession() == null ? 0 : device.getCurrentSession();
                    device.setCurrentSession(Math.max(0, current - 1));
                    deviceRepository.save(device);
                }

                // 3) Xóa session
                streamSessionRepository.delete(session);
            }

            page++;
        } while (!sessionsPage.isLast());

        // 4) Xóa stream
        streamRepository.delete(existingStream);
    }

    @Override
    @Transactional
    public Stream updateStream(Integer id, Stream stream) {
        Stream existing = streamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        if (stream.getName() != null) {
            existing.setName(stream.getName());
        }
        if (stream.getKeyStream() != null) {
            existing.setKeyStream(stream.getKeyStream());
        }
        if (stream.getVideoList() != null) {
            existing.setVideoList(stream.getVideoList());
        }
        if (stream.getTimeStart() != null) {
            existing.setTimeStart(stream.getTimeStart());
        }
        if (stream.getDuration() != null) {
            existing.setDuration(stream.getDuration());
        }

        return streamRepository.save(existing);
    }

    @Override
    public Page<Stream> getStreamsByChannelId(Integer channelId, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortObj);

        return streamRepository.findByChannelId(channelId, pageable);
    }

    private Sort parseSort(String sort) {
        // format: "timeStart,desc" hoặc "id,asc"
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;

            // whitelist field tránh lỗi runtime
            if (!isAllowedSortField(field))
                field = "id";

            return Sort.by(dir, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
    }

    private boolean isAllowedSortField(String field) {
        return field.equals("id")
                || field.equals("timeStart")
                || field.equals("name")
                || field.equals("duration");
    }

}
