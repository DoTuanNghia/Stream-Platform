package com.stream.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.stream.backend.entity.Stream;

public interface StreamRepository extends JpaRepository<Stream, Integer> {
    List<Stream> findByChannelId(Integer channelId);
    Stream save(Stream stream);
    void delete(Stream stream);
    Page<Stream> findByChannelId(Integer channelId, Pageable pageable);
}
