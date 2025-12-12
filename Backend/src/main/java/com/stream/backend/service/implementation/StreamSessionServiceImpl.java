package com.stream.backend.service.implementation;

import com.stream.backend.entity.Device;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.DeviceRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.FfmpegService;
import com.stream.backend.service.StreamSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public StreamSessionServiceImpl(
            StreamSessionRepository streamSessionRepository,
            StreamRepository streamRepository,
            DeviceRepository deviceRepository,
            FfmpegService ffmpegService) {
        this.streamSessionRepository = streamSessionRepository;
        this.streamRepository = streamRepository;
        this.deviceRepository = deviceRepository;
        this.ffmpegService = ffmpegService;
    }

    // ==========================
    // CRUD / QUERY CƠ BẢN
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
    public StreamSession creatStreamSession(
            StreamSession requestBody,
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
    public StreamSession getStreamSessionById(Integer streamSessionId) {
        return streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException("StreamSession not found with id = " + streamSessionId));
    }

    // ==========================
    // DỪNG STREAM SESSION
    // ==========================

    @Override
    @Transactional
    public StreamSession stopStreamSession(StreamSession session) {

        // 0. Dừng FFmpeg nếu đang chạy
        try {
            if (session != null && session.getStream() != null) {
                String streamKey = session.getStream().getKeyStream();
                System.out.println("[STOP] Stopping FFmpeg with streamKey = " + streamKey);
                ffmpegService.stopStream(streamKey);
            }
        } catch (Exception e) {
            System.err.println("Không thể dừng FFmpeg cho sessionId = " + session.getId());
            e.printStackTrace();
        }

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

    // Chuẩn hóa video source (Google Drive link -> direct link, v.v.)
    private String normalizeVideoSource(String raw) {
        if (raw == null)
            return null;
        raw = raw.trim();

        // Nếu là link Google Drive dạng /file/d/.../view thì convert sang direct link
        Pattern p = Pattern.compile("https?://drive\\.google\\.com/file/d/([^/]+)/view.*");
        Matcher m = p.matcher(raw);
        if (m.matches()) {
            String fileId = m.group(1);
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        }

        // Còn lại giữ nguyên (đường dẫn local, URL khác,...)
        return raw;
    }

    // ==========================
    // BẮT ĐẦU STREAM SESSION
    // ==========================

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

        Device device = devices.get(0); 

        // 3. Tạo session mới
        StreamSession session = new StreamSession();
        session.setStream(stream);
        session.setDevice(device);
        session.setStatus("ACTIVE");
        session.setSpecification("Blank");

        // Lưu ý:
        // - Thời gian bắt đầu (timeStart) và duration được lưu trong entity Stream
        //   (trường timeStart, duration), KHÔNG lưu trong StreamSession nữa.
        // - Ở đây chỉ tạo quan hệ giữa Stream và Device thông qua StreamSession.

        StreamSession saved = streamSessionRepository.save(session);

        // 4. Tăng currentSession trên device
        device.setCurrentSession(device.getCurrentSession() + 1);
        deviceRepository.save(device);

        // 5. Gọi FFmpeg để bắt đầu livestream lên YouTube
        try {
            String streamKey = stream.getKeyStream();
            if (streamKey == null || streamKey.isBlank()) {
                throw new RuntimeException("Stream key (keyStream) trống, không thể livestream.");
            }

            // Lấy dòng đầu tiên KHÔNG RỖNG trong videoList (mỗi dòng 1 video)
            String videoSource = null;
            if (stream.getVideoList() != null && !stream.getVideoList().isBlank()) {
                videoSource = Arrays.stream(stream.getVideoList().split("\\r?\\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(this::normalizeVideoSource)
                        .findFirst()
                        .orElse(null);
            }

            // videoSource có thể là:
            // - đường dẫn local: C:\Videos\demo.mp4
            // - URL Google Drive (direct link):
            //   https://drive.google.com/uc?export=download&id=...
            // - URL HTTP khác
            // Nếu videoSource == null => FfmpegService sẽ dùng demoVideo trong config
            ffmpegService.startStream(
                    videoSource,
                    null,
                    streamKey);
        } catch (Exception e) {
            System.err.println("Không thể khởi chạy FFmpeg cho streamId = " + streamId);
            e.printStackTrace();
        }

        return saved;
    }

    @Override
    @Transactional
    public StreamSession startScheduledSession(Integer streamSessionId) {
        StreamSession session = streamSessionRepository.findById(streamSessionId)
                .orElseThrow(() -> new RuntimeException(
                        "StreamSession not found with id = " + streamSessionId));

        if (!"SCHEDULED".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("StreamSession id=" + streamSessionId + " không ở trạng thái SCHEDULED");
        }

        Device device = session.getDevice();
        if (device == null) {
            throw new RuntimeException("StreamSession id=" + streamSessionId + " chưa gán device");
        }

        Stream stream = session.getStream();
        if (stream == null) {
            throw new RuntimeException("StreamSession id=" + streamSessionId + " chưa gán stream");
        }

        // 1. Cập nhật trạng thái session -> ACTIVE
        session.setStatus("ACTIVE");
        StreamSession saved = streamSessionRepository.save(session);

        // 2. Tăng currentSession của device
        int current = device.getCurrentSession() == null ? 0 : device.getCurrentSession();
        device.setCurrentSession(current + 1);
        deviceRepository.save(device);

        // 3. Gọi FFmpeg giống logic startSessionForStream
        try {
            String streamKey = stream.getKeyStream();
            if (streamKey == null || streamKey.isBlank()) {
                throw new RuntimeException("Stream key (keyStream) trống, không thể livestream.");
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

            ffmpegService.startStream(
                    videoSource,
                    null,
                    streamKey
            );
        } catch (Exception e) {
            System.err.println("Không thể khởi chạy FFmpeg cho scheduled StreamSession id = " + streamSessionId);
            e.printStackTrace();
        }

        return saved;
    }
}
