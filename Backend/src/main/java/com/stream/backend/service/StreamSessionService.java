package com.stream.backend.service;

import java.util.List;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionService {

    List<StreamSession> getAllStreamSessions();

    List<StreamSession> getStreamSessionsByDeviceId(Integer deviceId);

    List<StreamSession> getStreamSessionsByStreamId(Integer streamId);

    StreamSession creatStreamSession(StreamSession streamSession, Integer deviceId, Integer streamId);

    StreamSession stopStreamSession(StreamSession streamSession);

    StreamSession getStreamSessionById(Integer streamSessionId);

    StreamSession startSessionForStream(Integer streamId);

    StreamSession stopStreamSessionById(Integer sessionId);

    StreamSession create(StreamSession session);

    List<StreamSession> findPending();

    void markStarted(Integer id);   
}
