package com.stream.backend.scheduler;

import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.StreamSessionRepository;
import com.stream.backend.service.StreamSessionService;
import com.stream.backend.youtube.YouTubeLiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamScheduler {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamSessionService streamSessionService;
    private final YouTubeLiveService youTubeLiveService;

    /**
     * fixedDelay: chỉ chạy lượt tiếp theo sau khi lượt trước xong -> tránh overlap
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void autoStartAndStop() {
        LocalDateTime now = LocalDateTime.now();
        autoStartScheduledSessions(now);
        autoStopExpiredSessions(now);
    }

    /**
     * AUTO START:
     * - Session status = SCHEDULED
     * - stream.timeStart <= now
     * - Start FFmpeg trước (startScheduledSession)
     * - Sau đó có thể transition broadcast (tùy bạn, nhưng nên "tự động" theo encoder)
     */
    private void autoStartScheduledSessions(LocalDateTime now) {
        List<StreamSession> scheduledSessions = streamSessionRepository.findByStatus("SCHEDULED");

        for (StreamSession session : scheduledSessions) {
            Stream stream = session.getStream();
            if (stream == null || stream.getTimeStart() == null) continue;

            if (stream.getTimeStart().isAfter(now)) continue; // chưa tới giờ

            log.info("[AUTO-START] sessionId={}, streamId={}", session.getId(), stream.getId());

            try {
                // 1) Start encoder + set ACTIVE + set startedAt + tăng currentSession
                StreamSession started = streamSessionService.startScheduledSession(session.getId());

                // 2) Transition broadcast: KHÔNG BẮT BUỘC nếu bạn dùng autoStart (YouTube sẽ tự lên live khi có tín hiệu)
                // Nếu bạn vẫn muốn chủ động transition, nên "try" và ignore lỗi.
                try {
                    youTubeLiveService.transitionBroadcast(stream, "live");
                } catch (Exception ex) {
                    // Nhiều case YouTube từ chối transition khi chưa "ready/live" -> không làm sập scheduler
                    log.warn("[AUTO-START] transition 'live' failed for streamId={} (ignored): {}",
                            stream.getId(), ex.getMessage());
                }

            } catch (Exception e) {
                log.error("[AUTO-START] failed sessionId=" + session.getId(), e);
            }
        }
    }

    /**
     * AUTO STOP:
     * - Session status = ACTIVE
     * - duration = -1 => bỏ qua
     * - stop theo startedAt (chuẩn), fallback về stream.timeStart nếu startedAt null
     */
    private void autoStopExpiredSessions(LocalDateTime now) {
        List<StreamSession> activeSessions = streamSessionRepository.findByStatus("ACTIVE");

        for (StreamSession session : activeSessions) {
            Stream stream = session.getStream();
            if (stream == null) continue;

            Integer duration = stream.getDuration();
            if (duration == null) continue;

            if (duration == -1) continue; // live vô hạn

            // Ưu tiên startedAt của session, nếu chưa có thì fallback stream.timeStart
            LocalDateTime base = session.getStartedAt() != null ? session.getStartedAt() : stream.getTimeStart();
            if (base == null) continue;

            LocalDateTime endTime = base.plusMinutes(duration);

            if (endTime.isAfter(now)) continue; // chưa hết giờ

            log.info("[AUTO-STOP] sessionId={}, streamId={}", session.getId(), stream.getId());

            try {
                // 1) Stop encoder + set STOPPED + giảm currentSession + set stoppedAt
                streamSessionService.stopStreamSession(session);

                // 2) Transition complete (cũng nên ignore nếu fail)
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
    }
}
