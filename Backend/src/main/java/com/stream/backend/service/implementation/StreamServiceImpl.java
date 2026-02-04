package com.stream.backend.service.implementation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.stream.backend.entity.Member;
import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.MemberRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.FfmpegService;
import com.stream.backend.service.StreamService;
import com.stream.backend.youtube.YouTubeLiveService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StreamServiceImpl implements StreamService {

    private final StreamRepository streamRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final MemberRepository memberRepository;

    private final YouTubeLiveService youTubeLiveService;
    private final FfmpegService ffmpegService;

    public StreamServiceImpl(
            StreamRepository streamRepository,
            StreamSessionRepository streamSessionRepository,
            MemberRepository memberRepository,
            YouTubeLiveService youTubeLiveService,
            FfmpegService ffmpegService) {

        this.streamRepository = streamRepository;
        this.streamSessionRepository = streamSessionRepository;
        this.memberRepository = memberRepository;
        this.youTubeLiveService = youTubeLiveService;
        this.ffmpegService = ffmpegService;
    }

    @Override
    public Page<Stream> getStreamsByUserId(Integer userId, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortObj);

        return streamRepository.findByOwnerId(userId, pageable);
    }

    @Override
    public List<Stream> getStreamsByUserId(Integer userId) {
        return streamRepository.findByOwnerId(userId);
    }

    @Override
    public List<Stream> getStreamsByUserId(Integer userId, String sort) {
        Sort sortObj = parseSort(sort);
        return streamRepository.findByOwnerId(userId, sortObj);
    }

    @Override
    @Transactional
    public Stream createStream(Stream stream, Integer userId) {

        Member owner = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        stream.setOwner(owner);

        // (tuỳ bạn) nếu còn dùng Youtube schedule, bạn bật lại:
        // try {
        // String streamKey =
        // youTubeLiveService.createScheduledBroadcastForStream(stream);
        // stream.setKeyStream(streamKey);
        // } catch (Exception e) {
        // throw new RuntimeException("Không tạo được lịch YouTube", e);
        // }

        Stream saved = streamRepository.save(stream);

        // ✅ Kiểm tra đầy đủ các thuộc tính để tạo session SCHEDULED
        if (isStreamComplete(saved)) {
            StreamSession ss = streamSessionRepository
                    .findTopByStreamIdOrderByIdDesc(saved.getId())
                    .orElse(null);

            try {
                if (ss == null)
                    ss = new StreamSession();
                ss.setStream(saved);
                ss.setStatus("SCHEDULED");
                ss.setSpecification("Created/Scheduled");
                ss.setStartedAt(null);
                ss.setStoppedAt(null);
                streamSessionRepository.save(ss);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("[CREATE-STREAM] Race condition detected for stream {}, re-fetching session", saved.getId());
                ss = streamSessionRepository.findTopByStreamIdOrderByIdDesc(saved.getId()).orElse(null);
                if (ss != null && !"ACTIVE".equalsIgnoreCase(ss.getStatus())) {
                    ss.setStatus("SCHEDULED");
                    ss.setSpecification("Created/Scheduled (race condition resolved)");
                    streamSessionRepository.save(ss);
                }
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public List<Stream> createStreamsBulk(List<Stream> streams, Integer userId) {
        if (streams == null || streams.isEmpty()) {
            return List.of();
        }

        Member owner = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Stream> toSave = new ArrayList<>();
        for (Stream s : streams) {
            if (s == null)
                continue;

            String name = s.getName();
            String key = s.getKeyStream();
            if (name == null || name.isBlank())
                continue;
            if (key == null || key.isBlank())
                continue;

            Stream st = new Stream();
            st.setOwner(owner);
            st.setName(name.trim());
            st.setKeyStream(key.trim());
            // các field khác để null (video/timeStart/duration...)
            st.setVideoList(null);
            st.setTimeStart(null);
            st.setDuration(null);

            toSave.add(st);
        }

        if (toSave.isEmpty()) {
            return List.of();
        }

        return streamRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void deleteStream(Stream stream) {

        Stream existingStream = streamRepository.findById(stream.getId())
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        // Xóa session (nếu ACTIVE thì stop FFmpeg + complete youtube)
        StreamSession ss = streamSessionRepository
                .findTopByStreamIdOrderByIdDesc(existingStream.getId())
                .orElse(null);

        if (ss != null) {
            if ("ACTIVE".equalsIgnoreCase(ss.getStatus())) {
                String streamKey = existingStream.getKeyStream();
                if (streamKey != null && !streamKey.isBlank()) {
                    ffmpegService.stopStream(streamKey);
                }
                try {
                    youTubeLiveService.transitionBroadcast(existingStream, "complete");
                } catch (Exception ignore) {
                }
            }
            streamSessionRepository.delete(ss);
        }

        streamRepository.delete(existingStream);
    }

    @Override
    @Transactional
    public Stream updateStream(Integer id, Stream stream) {

        Stream existing = streamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        // ✅ Kiểm tra trạng thái STOPPED trước khi cập nhật
        StreamSession ss = streamSessionRepository
                .findTopByStreamIdOrderByIdDesc(id)
                .orElse(null);

        boolean isStoppedOrError = ss != null && ("STOPPED".equalsIgnoreCase(ss.getStatus())
                || "ERROR".equalsIgnoreCase(ss.getStatus()));

        // ✅ Luôn cập nhật name và keyStream
        if (stream.getName() != null)
            existing.setName(stream.getName());
        if (stream.getKeyStream() != null)
            existing.setKeyStream(stream.getKeyStream());

        // ✅ Nếu stream đang STOPPED hoặc ERROR → chỉ giữ name và keyStream, reset các
        // trường khác
        if (isStoppedOrError) {
            log.info("[UPDATE-STREAM] Stream {} is STOPPED/ERROR, resetting all fields except name and keyStream", id);

            // Xóa video cũ nếu có
            String oldVideoList = existing.getVideoList();
            if (oldVideoList != null && !oldVideoList.isBlank()) {
                deleteOldVideoFiles(oldVideoList, null);
            }

            // Reset các trường về null
            existing.setVideoList(null);
            existing.setTimeStart(null);
            existing.setDuration(null);

            // Reset session về SCHEDULED
            ss.setStatus("SCHEDULED");
            ss.setSpecification("Edited -> rescheduled (reset from STOPPED)");
            ss.setStartedAt(null);
            ss.setStoppedAt(null);
            ss.setLastError(null);
            ss.setLastErrorAt(null);
            streamSessionRepository.save(ss);
        } else {
            // ✅ Nếu stream không phải STOPPED → cập nhật bình thường

            // ✅ XÓA VIDEO CŨ KHI UPDATE VIDEOLIST MỚI
            // Xử lý cả trường hợp FE gửi empty string "" (coi như null)
            String newVideoList = stream.getVideoList();
            boolean newVideoIsEmpty = newVideoList == null || newVideoList.trim().isEmpty();

            String oldVideoList = existing.getVideoList();

            if (newVideoIsEmpty) {
                // FE muốn xóa video → xóa file cũ và set null
                if (oldVideoList != null && !oldVideoList.isBlank()) {
                    deleteOldVideoFiles(oldVideoList, null);
                }
                existing.setVideoList(null);
            } else {
                // FE gửi video mới
                if (oldVideoList != null && !oldVideoList.isBlank() && !oldVideoList.equals(newVideoList)) {
                    deleteOldVideoFiles(oldVideoList, newVideoList);
                }
                existing.setVideoList(newVideoList);
            }

            // ✅ cho phép update cả null (khi FE gửi null)
            existing.setTimeStart(stream.getTimeStart());
            existing.setDuration(stream.getDuration());

            // ✅ Kiểm tra lại trạng thái stream sau khi cập nhật
            boolean isComplete = isStreamComplete(existing);

            if (ss == null) {
                // Chưa có session
                if (isComplete) {
                    // ✅ FIX RACE CONDITION: Re-fetch session để đảm bảo không có session được tạo
                    // bởi request khác
                    ss = streamSessionRepository.findTopByStreamIdOrderByIdDesc(id).orElse(null);
                    if (ss == null) {
                        try {
                            // Stream đủ điều kiện → tạo SCHEDULED
                            StreamSession newSs = new StreamSession();
                            newSs.setStream(existing);
                            newSs.setStatus("SCHEDULED");
                            newSs.setSpecification("Edited -> scheduled");
                            streamSessionRepository.save(newSs);
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            // ✅ Nếu gặp duplicate key → re-fetch và update session
                            log.warn("[UPDATE-STREAM] Race condition detected for stream {}, re-fetching session", id);
                            ss = streamSessionRepository.findTopByStreamIdOrderByIdDesc(id).orElse(null);
                            if (ss != null && !"ACTIVE".equalsIgnoreCase(ss.getStatus())) {
                                ss.setStatus("SCHEDULED");
                                ss.setSpecification("Edited -> scheduled (race condition resolved)");
                                streamSessionRepository.save(ss);
                            }
                        }
                    } else {
                        // Session đã được tạo bởi request khác → update thay vì tạo mới
                        if (!"ACTIVE".equalsIgnoreCase(ss.getStatus())) {
                            ss.setStatus("SCHEDULED");
                            ss.setSpecification("Edited -> scheduled");
                            streamSessionRepository.save(ss);
                        }
                    }
                }
                // Nếu không đủ điều kiện và chưa có session → không làm gì
            } else {
                // Đã có session
                if (isComplete) {
                    // Stream đủ điều kiện → giữ hoặc set SCHEDULED (nếu chưa phải SCHEDULED/ACTIVE)
                    if (!"SCHEDULED".equalsIgnoreCase(ss.getStatus()) && !"ACTIVE".equalsIgnoreCase(ss.getStatus())) {
                        ss.setStatus("SCHEDULED");
                        ss.setSpecification("Edited -> rescheduled");
                        streamSessionRepository.save(ss);
                    }
                } else {
                    // Stream KHÔNG đủ điều kiện → set về NONE (không xóa session, chỉ đổi status)
                    if ("SCHEDULED".equalsIgnoreCase(ss.getStatus())) {
                        ss.setStatus("NONE");
                        ss.setSpecification("Edited -> incomplete (missing required fields)");
                        streamSessionRepository.save(ss);
                        log.info("[UPDATE-STREAM] Stream {} is now incomplete, changed status from SCHEDULED to NONE",
                                id);
                    }
                }
            }
        }

        Stream saved = streamRepository.save(existing);

        return saved;
    }

    /**
     * Xóa file video cũ khi cập nhật videoList mới
     * Chỉ xóa file local, không xóa URL
     */
    private void deleteOldVideoFiles(String oldVideoList, String newVideoList) {
        if (oldVideoList == null || oldVideoList.isBlank()) {
            return;
        }

        String[] oldPaths = oldVideoList.split("\\r?\\n");
        for (String videoPath : oldPaths) {
            videoPath = videoPath.trim();
            if (videoPath.isEmpty())
                continue;

            // Kiểm tra nếu video cũ cũng có trong video mới → không xóa
            if (newVideoList != null && newVideoList.contains(videoPath)) {
                log.info("[UPDATE-STREAM] Skipping delete (same video): {}", videoPath);
                continue;
            }

            // Chỉ xóa file local (không phải URL)
            if (isLocalFile(videoPath)) {
                try {
                    File videoFile = new File(videoPath);
                    if (videoFile.exists()) {
                        boolean deleted = videoFile.delete();
                        if (deleted) {
                            log.info("[UPDATE-STREAM] Deleted old video file: {}", videoPath);
                        } else {
                            log.warn("[UPDATE-STREAM] Failed to delete old video file: {}", videoPath);
                        }
                    } else {
                        log.info("[UPDATE-STREAM] Old video file not found (may already deleted): {}", videoPath);
                    }
                } catch (Exception e) {
                    log.error("[UPDATE-STREAM] Error deleting old video file: {}", videoPath, e);
                }
            } else {
                log.info("[UPDATE-STREAM] Skipping URL (not local file): {}", videoPath);
            }
        }
    }

    /**
     * Kiểm tra xem đường dẫn có phải là file local không
     */
    private boolean isLocalFile(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        // Kiểm tra Windows path (C:\...) hoặc Unix path (/...)
        // Không phải URL (http://, https://)
        return !path.startsWith("http://")
                && !path.startsWith("https://")
                && (path.matches("^[a-zA-Z]:\\\\.*") || path.startsWith("/"));
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
        return field.equals("id")
                || field.equals("timeStart")
                || field.equals("name")
                || field.equals("duration");
    }

    /**
     * Kiểm tra stream có đầy đủ các thuộc tính cần thiết để được SCHEDULED
     * Các thuộc tính cần thiết: videoList, timeStart, duration, keyStream
     */
    private boolean isStreamComplete(Stream stream) {
        if (stream == null) {
            return false;
        }

        // Kiểm tra videoList
        String videoList = stream.getVideoList();
        boolean hasVideoList = videoList != null && !videoList.trim().isEmpty();

        // Kiểm tra timeStart
        boolean hasTimeStart = stream.getTimeStart() != null;

        // Kiểm tra duration (phải khác null VÀ khác 0)
        boolean hasDuration = stream.getDuration() != null && stream.getDuration() != 0;

        // Kiểm tra keyStream
        String keyStream = stream.getKeyStream();
        boolean hasKeyStream = keyStream != null && !keyStream.trim().isEmpty();

        return hasVideoList && hasTimeStart && hasDuration && hasKeyStream;
    }
}
