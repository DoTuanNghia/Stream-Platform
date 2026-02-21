package com.stream.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.stream.backend.entity.Stream;

public interface StreamRepository extends JpaRepository<Stream, Integer> {

    Stream save(Stream stream);

    void delete(Stream stream);

    Page<Stream> findByOwnerId(Integer ownerId, Pageable pageable);

    List<Stream> findByOwnerId(Integer ownerId);

    List<Stream> findByOwnerId(Integer ownerId, Sort sort);

    /**
     * Lấy tất cả videoList từ các stream có session SCHEDULED hoặc ACTIVE
     * Dùng để tạo danh sách bảo vệ khi dọn dẹp video mồ côi
     */
    @Query("""
            SELECT s.videoList FROM Stream s
            WHERE s.videoList IS NOT NULL AND s.videoList <> ''
            AND EXISTS (
                SELECT 1 FROM StreamSession ss
                WHERE ss.stream = s
                AND LOWER(ss.status) IN ('scheduled', 'active')
            )
        """)
    List<String> findAllActiveVideoLists();
}
