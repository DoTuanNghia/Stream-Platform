package com.stream.backend.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Integer>{
    List<StreamSession> findAll();
    List<StreamSession> findByDeviceId(Integer deviceId);
    List<StreamSession> findByStreamId(Integer streamId);
    StreamSession save(StreamSession streamSession);
    void delete(StreamSession streamSession);
    List<StreamSession> findByStatus(String status);
}
