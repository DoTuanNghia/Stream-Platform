package com.stream.backend.scheduler;

import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.StreamSessionService;
import com.stream.backend.youtube.YouTubeLiveService;
import lombok.extern.slf4j.Slf4j;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class StreamScheduler {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamSessionService streamSessionService;
    private final StreamRepository streamRepository;
    private final YouTubeLiveService youTubeLiveService;

    private static final String VIDEO_DIR = "D:\\videos";

    public StreamScheduler(
            StreamSessionRepository streamSessionRepository,
            StreamSessionService streamSessionService,
            StreamRepository streamRepository,
            YouTubeLiveService youTubeLiveService) {
        this.streamSessionRepository = streamSessionRepository;
        this.streamSessionService = streamSessionService;
        this.streamRepository = streamRepository;
        this.youTubeLiveService = youTubeLiveService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void autoStartAndStop() {
        LocalDateTime now = LocalDateTime.now();
        autoStartScheduledSessions(now);
        autoStopExpiredSessions(now);
    }

    /**
     * Tự động xóa file video sau 24h khi stream ở trạng thái STOPPED
     * Chạy mỗi 5 phút để kiểm tra
     */
    @Scheduled(fixedDelay = 300_000)
    public void autoDeleteStoppedVideos() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(5);

        int page = 0;
        int size = 200;

        Page<StreamSession> p;
        do {
            p = streamSessionRepository.findByStatusIgnoreCase(
                    "STOPPED",
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));

            for (StreamSession session : p.getContent()) {
                LocalDateTime stoppedAt = session.getStoppedAt();
                if (stoppedAt == null) {
                    continue;
                }

                if (stoppedAt.isAfter(threshold)) {
                    continue;
                }

                Stream stream = session.getStream();
                if (stream == null) {
                    continue;
                }

                log.info("[AUTO-DELETE-VIDEO] sessionId={}, streamId={}, stoppedAt={}",
                        session.getId(), stream.getId(), stoppedAt);

                try {
                    streamSessionService.deleteVideoAndResetStream(session);
                } catch (Exception e) {
                    log.error("[AUTO-DELETE-VIDEO] Failed sessionId=" + session.getId(), e);
                }
            }

            page++;
        } while (!p.isLast());
    }

    /**
     * Tự động dọn dẹp video mồ côi trong thư mục video
     * Chạy mỗi 3 ngày (259.200.000ms)
     *
     * Logic an toàn:
     * 1. Lấy tất cả videoList từ các stream SCHEDULED/ACTIVE → danh sách bảo vệ
     * 2. Scan thư mục video → lấy tất cả file .mp4
     * 3. Chỉ xóa file KHÔNG nằm trong danh sách bảo vệ
     * 4. Nếu không lấy được danh sách bảo vệ (lỗi DB) → HỦY, không xóa gì cả
     */
    @Scheduled(fixedDelay = 259_200_000) // 3 ngày
    public void autoCleanOrphanedVideos() {
        log.info("[CLEAN-ORPHANED] Bắt đầu dọn dẹp video mồ côi trong: {}", VIDEO_DIR);

        File dir = new File(VIDEO_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("[CLEAN-ORPHANED] Thư mục không tồn tại: {}", VIDEO_DIR);
            return;
        }

        // Bước 1: Lấy danh sách video đang được bảo vệ (SCHEDULED / ACTIVE)
        Set<String> protectedFiles = new HashSet<>();
        try {
            List<String> activeVideoLists = streamRepository.findAllActiveVideoLists();
            for (String videoList : activeVideoLists) {
                if (videoList == null || videoList.isBlank()) continue;

                // videoList có thể chứa nhiều đường dẫn, mỗi dòng 1 đường dẫn
                String[] paths = videoList.split("\\r?\\n");
                for (String path : paths) {
                    path = path.trim();
                    if (path.isEmpty()) continue;

                    // Lấy tên file từ đường dẫn đầy đủ
                    // VD: D:\videos\210226_01.mp4 → 210226_01.mp4
                    File f = new File(path);
                    protectedFiles.add(f.getName().toLowerCase());
                }
            }
        } catch (Exception e) {
            // AN TOÀN: Nếu lỗi DB → KHÔNG xóa gì cả
            log.error("[CLEAN-ORPHANED] Lỗi lấy danh sách bảo vệ, HỦY dọn dẹp", e);
            return;
        }

        log.info("[CLEAN-ORPHANED] Video được bảo vệ: {} file", protectedFiles.size());
        if (!protectedFiles.isEmpty()) {
            log.info("[CLEAN-ORPHANED] Danh sách bảo vệ: {}", protectedFiles);
        }

        // Bước 2: Scan thư mục video
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp4"));
        if (files == null || files.length == 0) {
            log.info("[CLEAN-ORPHANED] Không có file .mp4 nào trong thư mục");
            return;
        }

        // Bước 3: Xóa file mồ côi
        int deletedCount = 0;
        int protectedCount = 0;
        long freedBytes = 0;

        for (File file : files) {
            String fileName = file.getName().toLowerCase();

            if (protectedFiles.contains(fileName)) {
                protectedCount++;
                log.debug("[CLEAN-ORPHANED] GIỮ LẠI: {}", file.getName());
                continue;
            }

            // File không nằm trong danh sách bảo vệ → xóa
            long fileSize = file.length();
            try {
                if (file.delete()) {
                    deletedCount++;
                    freedBytes += fileSize;
                    log.info("[CLEAN-ORPHANED] ĐÃ XÓA: {} ({}KB)", file.getName(), fileSize / 1024);
                } else {
                    log.warn("[CLEAN-ORPHANED] KHÔNG THỂ XÓA: {}", file.getName());
                }
            } catch (Exception e) {
                log.error("[CLEAN-ORPHANED] Lỗi xóa: {}", file.getName(), e);
            }
        }

        log.info("[CLEAN-ORPHANED] Hoàn tất: xóa {} file ({} MB), giữ {} file đang dùng",
                deletedCount, freedBytes / (1024 * 1024), protectedCount);
    }

    private void autoStartScheduledSessions(LocalDateTime now) {
        int page = 0;
        int size = 200;

        Page<StreamSession> p;
        do {
            p = streamSessionRepository.findByStatusIgnoreCase(
                    "SCHEDULED",
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));

            for (StreamSession session : p.getContent()) {
                Stream stream = session.getStream();
                if (stream == null || stream.getTimeStart() == null)
                    continue;
                if (stream.getTimeStart().isAfter(now))
                    continue;

                log.info("[AUTO-START] sessionId={}, streamId={}", session.getId(), stream.getId());

                try {
                    StreamSession started = streamSessionService.startScheduledSession(session.getId());

                    if (started != null && "ACTIVE".equalsIgnoreCase(started.getStatus())) {
                        try {
                            youTubeLiveService.transitionBroadcast(stream, "live");
                        } catch (Exception ex) {
                            log.warn("[AUTO-START] transition 'live' failed for streamId={} (ignored): {}",
                                    stream.getId(), ex.getMessage());
                        }
                    } else {
                        log.warn("[AUTO-START] sessionId={} not ACTIVE (status={})",
                                session.getId(), started != null ? started.getStatus() : "null");
                    }

                } catch (Exception e) {
                    log.error("[AUTO-START] failed sessionId=" + session.getId(), e);
                }
            }

            page++;
        } while (!p.isLast());
    }

    private void autoStopExpiredSessions(LocalDateTime now) {
        int page = 0;
        int size = 200;

        Page<StreamSession> p;
        do {
            p = streamSessionRepository.findByStatusIgnoreCase(
                    "ACTIVE",
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));

            for (StreamSession session : p.getContent()) {
                Stream stream = session.getStream();
                if (stream == null)
                    continue;

                Integer duration = stream.getDuration();
                if (duration == null)
                    continue;
                if (duration == -1)
                    continue;

                LocalDateTime base = session.getStartedAt() != null ? session.getStartedAt() : stream.getTimeStart();
                if (base == null)
                    continue;

                LocalDateTime endTime = base.plusMinutes(duration);
                if (endTime.isAfter(now))
                    continue;

                log.info("[AUTO-STOP] sessionId={}, streamId={}", session.getId(), stream.getId());

                try {
                    streamSessionService.stopStreamSession(session);

                    try {
                        youTubeLiveService.transitionBroadcast(stream, "complete");
                    } catch (Exception ex) {
                        log.warn("[AUTO-STOP] transition 'complete' failed for streamId={} (ignored): {}",
                                stream.getId(), ex.getMessage());
                    }
                } catch (Exception e) {
                    log.error("[AUTO-STOP] failed sessionId=" + session.getId(), e);
                }
            }

            page++;
        } while (!p.isLast());
    }
}
