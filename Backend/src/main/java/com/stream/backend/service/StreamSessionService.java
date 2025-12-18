package com.stream.backend.service;

import java.util.List;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionService {

    List<StreamSession> getAllStreamSessions();

    List<StreamSession> getStreamSessionsByDeviceId(Integer deviceId);

    List<StreamSession> getStreamSessionsByStreamId(Integer streamId);

    StreamSession createStreamSession(StreamSession streamSession, Integer deviceId, Integer streamId);

    StreamSession stopStreamSession(StreamSession streamSession);

    StreamSession getStreamSessionById(Integer streamSessionId);

    StreamSession startSessionForStream(Integer streamId);

    StreamSession startScheduledSession(Integer streamSessionId);
}
