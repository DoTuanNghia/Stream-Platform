package com.stream.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionService {

    Page<StreamSession> getAllStreamSessions(Integer userId, int page, int size, String sort);

    Page<StreamSession> getStreamSessionsByStreamId(Integer streamId, int page, int size, String sort);

    Map<Integer, String> getStatusMapByStreamIds(List<Integer> streamIds);

    StreamSession stopStreamSession(StreamSession streamSession);

    StreamSession getStreamSessionById(Integer streamSessionId);

    StreamSession startSessionForStream(Integer streamId);

    StreamSession startScheduledSession(Integer streamSessionId);

    Page<StreamSession> adminGetAll(String status, int page, int size, String sort);

    List<StreamSession> getAllStreamSessionsList(Integer userId, String sort);
}
