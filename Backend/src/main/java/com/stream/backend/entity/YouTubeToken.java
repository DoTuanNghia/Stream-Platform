package com.stream.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "youtube_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class YouTubeToken {

  @Id
  @Column(name = "token_key", length = 100)
  private String tokenKey; // ví dụ "default" hoặc sau này là memberId

  @Lob
  @Column(name = "access_token")
  private String accessToken;

  @Lob
  @Column(name = "refresh_token")
  private String refreshToken;

  @Column(name = "expiry_time_ms")
  private Long expiryTimeMs;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
