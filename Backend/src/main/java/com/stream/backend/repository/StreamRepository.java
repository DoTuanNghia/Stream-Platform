package com.stream.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stream.backend.entity.Stream;

public interface StreamRepository extends JpaRepository<Stream, Integer> {
    List<Stream> findByChannelId(Integer channelId);
    Stream save(Stream stream);
}
