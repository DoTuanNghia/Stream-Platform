package com.stream.backend.scheduler;

import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.StreamSessionService;
import com.stream.backend.youtube.YouTubeLiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamScheduler {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamSessionService streamSessionService;
    private final YouTubeLiveService youTubeLiveService;

    @Scheduled(fixedDelay = 10_000)
    public void autoStartAndStop() { // ✅ bỏ @Transactional để tránh rollback-only
        LocalDateTime now = LocalDateTime.now();
        autoStartScheduledSessions(now);
        autoStopExpiredSessions(now);
    }

    private void autoStartScheduledSessions(LocalDateTime now) {
        int page = 0;
        int size = 200;

        Page<StreamSession> p;
        do {
            p = streamSessionRepository.findByStatusIgnoreCase(
                    "SCHEDULED",
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
            );

            for (StreamSession session : p.getContent()) {
                Stream stream = session.getStream();
                if (stream == null || stream.getTimeStart() == null) continue;
                if (stream.getTimeStart().isAfter(now)) continue;

                log.info("[AUTO-START] sessionId={}, streamId={}", session.getId(), stream.getId());

                try {
                    StreamSession started = streamSessionService.startScheduledSession(session.getId());

                    // ✅ nếu fail thì service đã set ERROR, không transition youtube nữa
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
                    // trường hợp hiếm nếu service có lỗi ngoài 예상
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
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
            );

            for (StreamSession session : p.getContent()) {
                Stream stream = session.getStream();
                if (stream == null) continue;

                Integer duration = stream.getDuration();
                if (duration == null) continue;
                if (duration == -1) continue;

                LocalDateTime base = session.getStartedAt() != null ? session.getStartedAt() : stream.getTimeStart();
                if (base == null) continue;

                LocalDateTime endTime = base.plusMinutes(duration);
                if (endTime.isAfter(now)) continue;

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
