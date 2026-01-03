package com.stream.backend.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.stream.backend.entity.YouTubeToken;
import com.stream.backend.repository.YouTubeTokenRepository;

@Service
public class YouTubeTokenService {

  private final YouTubeTokenRepository repo;

  public YouTubeTokenService(YouTubeTokenRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public YouTubeToken saveFromTokenResponse(String tokenKey, TokenResponse tr) {
    YouTubeToken t = repo.findById(tokenKey).orElseGet(YouTubeToken::new);
    t.setTokenKey(tokenKey);

    t.setAccessToken(tr.getAccessToken());

    // Google chỉ trả refresh_token ở lần đầu hoặc khi approvalPrompt=force
    if (tr.getRefreshToken() != null && !tr.getRefreshToken().isBlank()) {
      t.setRefreshToken(tr.getRefreshToken());
    }

    // expiresInSeconds -> expiryTimeMs
    if (tr.getExpiresInSeconds() != null) {
      long expiryMs = System.currentTimeMillis() + tr.getExpiresInSeconds() * 1000L;
      t.setExpiryTimeMs(expiryMs);
    }

    t.setUpdatedAt(Instant.now());
    return repo.save(t);
  }

  public Optional<YouTubeToken> get(String tokenKey) {
    return repo.findById(tokenKey);
  }
}
