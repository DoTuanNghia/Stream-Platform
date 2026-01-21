package com.stream.backend.service.implementation;

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

        // ✅ nếu có timeStart => tạo/ghi đè session SCHEDULED
        if (saved.getTimeStart() != null) {
            StreamSession ss = streamSessionRepository
                    .findTopByStreamIdOrderByIdDesc(saved.getId())
                    .orElse(null);

            if (ss == null)
                ss = new StreamSession();
            ss.setStream(saved);
            ss.setStatus("SCHEDULED");
            ss.setSpecification("Created/Scheduled");
            ss.setStartedAt(null);
            ss.setStoppedAt(null);
            streamSessionRepository.save(ss);
        }

        return saved;
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

        if (stream.getName() != null)
            existing.setName(stream.getName());
        if (stream.getKeyStream() != null)
            existing.setKeyStream(stream.getKeyStream());
        if (stream.getVideoList() != null)
            existing.setVideoList(stream.getVideoList());

        // ✅ cho phép update cả null (khi FE gửi null)
        existing.setTimeStart(stream.getTimeStart());
        existing.setDuration(stream.getDuration());

        Stream saved = streamRepository.save(existing);

        // ✅ Nếu stream đang STOPPED mà user sửa lại -> chuyển về SCHEDULED
        StreamSession ss = streamSessionRepository
                .findTopByStreamIdOrderByIdDesc(id)
                .orElse(null);

        if (ss != null && ("STOPPED".equalsIgnoreCase(ss.getStatus())
                || "ERROR".equalsIgnoreCase(ss.getStatus()))) {
            ss.setStatus("SCHEDULED");
            ss.setSpecification("Edited -> rescheduled");
            ss.setStartedAt(null);
            ss.setStoppedAt(null);
            ss.setLastError(null);
            ss.setLastErrorAt(null);
            streamSessionRepository.save(ss);
        }

        // ✅ Nếu chưa có session mà có timeStart => tạo SCHEDULED
        if (ss == null && saved.getTimeStart() != null) {
            StreamSession newSs = new StreamSession();
            newSs.setStream(saved);
            newSs.setStatus("SCHEDULED");
            newSs.setSpecification("Edited -> scheduled");
            streamSessionRepository.save(newSs);
        }

        return saved;
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
}
