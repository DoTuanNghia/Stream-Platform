package com.stream.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Integer> {

    // Paging
    Page<StreamSession> findAll(Pageable pageable);

    Page<StreamSession> findByDeviceId(Integer deviceId, Pageable pageable);

    Page<StreamSession> findByStreamId(Integer streamId, Pageable pageable);

    // ACTIVE-only (phân trang cho trang StreamSession)
    Page<StreamSession> findByStatusIgnoreCase(String status, Pageable pageable);

    Optional<StreamSession> findFirstByStreamId(Integer streamId);

    boolean existsByStreamId(Integer streamId);

    @Query("""
                select ss
                from StreamSession ss
                join ss.stream s
                join s.channel c
                join c.user u
                where lower(ss.status) = 'active'
                  and (:userId is null or u.id = :userId)
            """)
    Page<StreamSession> findActiveByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("""
                select ss
                from StreamSession ss
                where (:status is null or lower(ss.status) = lower(:status))
            """)
    Page<StreamSession> findAllByOptionalStatus(@Param("status") String status, Pageable pageable);
}
