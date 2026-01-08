package com.stream.backend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.stream.backend.service.YouTubeTokenService;

@RestController
@RequestMapping("/api/youtube")
@CrossOrigin(origins = {
  "http://localhost:5173",
  "https://stream-platform-eta.vercel.app"
})
public class YouTubeAuthController {

  private final GoogleAuthorizationCodeFlow flow;
  private final YouTubeTokenService tokenService;

  @Value("${youtube.redirectUri}")
  private String redirectUri;

  @Value("${app.frontend.url:https://stream-platform-eta.vercel.app}")
  private String frontendUrl;

  public YouTubeAuthController(GoogleAuthorizationCodeFlow flow, YouTubeTokenService tokenService) {
    this.flow = flow;
    this.tokenService = tokenService;
  }

  // FE gọi endpoint này để lấy URL google login
  @GetMapping("/auth-url")
  public Map<String, String> getAuthUrl() {
    String url = flow.newAuthorizationUrl()
      .setRedirectUri(redirectUri)
      .setAccessType("offline")
      .setApprovalPrompt("force") // để có refresh_token chắc chắn
      .build();

    return Map.of("url", url);
  }

  // Google redirect về đây sau khi user consent
  @GetMapping("/oauth2callback")
  public ResponseEntity<?> callback(@RequestParam("code") String code) throws IOException {

    TokenResponse tokenResponse = flow.newTokenRequest(code)
      .setRedirectUri(redirectUri)
      .execute();

    // Tạm thời lưu theo key "default"
    tokenService.saveFromTokenResponse("default", tokenResponse);

    // Redirect về FE sau khi connect thành công (đỡ hiện trang trắng)
    return ResponseEntity.status(302)
      .header("Location", frontendUrl + "/youtube-connected")
      .build();
  }

  // Endpoint test: xem đã có token chưa
  @GetMapping("/status")
  public Map<String, Object> status() {
    boolean connected = tokenService.get("default").isPresent();
    return Map.of("connected", connected);
  }
}
