package com.stream.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.stream.backend.entity.YouTubeToken;

public interface YouTubeTokenRepository extends JpaRepository<YouTubeToken, String> {
}
