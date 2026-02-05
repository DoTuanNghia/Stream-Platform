package com.stream.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import java.util.List;

import com.stream.backend.entity.StreamSession;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Integer> {

  Page<StreamSession> findAll(Pageable pageable);

  Page<StreamSession> findByStreamId(Integer streamId, Pageable pageable);

  Page<StreamSession> findByStatusIgnoreCase(String status, Pageable pageable);

  // ✅ stream_id unique nhưng vẫn thêm order để lấy “mới nhất” cho chắc
  Optional<StreamSession> findTopByStreamIdOrderByIdDesc(Integer streamId);

  boolean existsByStreamId(Integer streamId);

  long countByStatusIgnoreCase(String status);

  @Query("""
         select ss
         from StreamSession ss
         join ss.stream s
         join s.owner u
         where lower(ss.status) in ('active','scheduled','error')
           and (:userId is null or u.id = :userId)
      """)
  Page<StreamSession> findActiveOrScheduledByUserId(@Param("userId") Integer userId, Pageable pageable);

  @Query("""
         select ss
         from StreamSession ss
         left join ss.stream s
         where (:status is null or lower(ss.status) = lower(:status))
      """)
  Page<StreamSession> findAllByOptionalStatus(@Param("status") String status, Pageable pageable);

  @Query("""
         select ss
         from StreamSession ss
         left join ss.stream s
         left join s.owner u
         where (:status is null or lower(ss.status) = lower(:status))
           and (:ownerName is null or u.name = :ownerName)
      """)
  Page<StreamSession> findAllByOptionalStatusAndOwnerName(@Param("status") String status,
      @Param("ownerName") String ownerName, Pageable pageable);

  @Query("""
         select ss
         from StreamSession ss
         join ss.stream s
         join s.owner u
         where lower(ss.status) in ('active','scheduled','error')
           and (:userId is null or u.id = :userId)
      """)
  List<StreamSession> findActiveOrScheduledByUserIdList(@Param("userId") Integer userId, Sort sort);
}
