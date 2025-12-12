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

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoStartAndStop() {
        LocalDateTime now = LocalDateTime.now();
        autoStartScheduledSessions(now);
        autoStopExpiredSessions(now);
    }

    private void autoStartScheduledSessions(LocalDateTime now) {
        List<StreamSession> scheduledSessions = streamSessionRepository.findByStatus("SCHEDULED");
        for (StreamSession session : scheduledSessions) {
            Stream stream = session.getStream();
            if (stream == null || stream.getTimeStart() == null) continue;

            if (!stream.getTimeStart().isAfter(now)) {
                log.info("[AUTO-START] sessionId={}, streamId={}", session.getId(), stream.getId());
                try {
                    // 1. Chuyển broadcast sang LIVE (nếu có)
                    youTubeLiveService.transitionBroadcast(stream, "live");

                    // 2. Start FFmpeg + set status ACTIVE, tăng currentSession
                    streamSessionService.startScheduledSession(session.getId());
                } catch (Exception e) {
                    log.error("Lỗi auto-start cho sessionId=" + session.getId(), e);
                }
            }
        }
    }

    private void autoStopExpiredSessions(LocalDateTime now) {
        List<StreamSession> activeSessions = streamSessionRepository.findByStatus("ACTIVE");
        for (StreamSession session : activeSessions) {
            Stream stream = session.getStream();
            if (stream == null || stream.getTimeStart() == null || stream.getDuration() == null) continue;

            if (stream.getDuration() == -1) {
                // live vô hạn, không auto stop
                continue;
            }

            LocalDateTime endTime = stream.getTimeStart().plusMinutes(stream.getDuration());
            if (!endTime.isAfter(now)) {
                log.info("[AUTO-STOP] sessionId={}, streamId={}", session.getId(), stream.getId());
                try {
                    // 1. Stop FFmpeg + set status STOPPED, giảm currentSession
                    streamSessionService.stopStreamSession(session);

                    // 2. Chuyển broadcast sang COMPLETED
                    youTubeLiveService.transitionBroadcast(stream, "complete");
                } catch (Exception e) {
                    log.error("Lỗi auto-stop cho sessionId=" + session.getId(), e);
                }
            }
        }
    }
}
