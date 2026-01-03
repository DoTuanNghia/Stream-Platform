package com.stream.backend.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

@Configuration
public class YouTubeClientConfig {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final List<String> SCOPES =
      Collections.singletonList("https://www.googleapis.com/auth/youtube");

  @Value("${youtube.credentials.path:classpath:youtube-client-secret.json}")
  private String credentialsPath;

  @Bean
  public NetHttpTransport netHttpTransport() throws Exception {
    return GoogleNetHttpTransport.newTrustedTransport();
  }

  @Bean
  public GoogleClientSecrets googleClientSecrets() throws IOException {
    String resourceName = credentialsPath.replace("classpath:", "");
    InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
    if (in == null) {
      throw new IOException("Không tìm thấy file credentials: " + credentialsPath);
    }
    return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
  }

  @Bean
  public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(
      NetHttpTransport httpTransport,
      GoogleClientSecrets clientSecrets
  ) {
    return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
      .setAccessType("offline")
      // approvalPrompt("force") sẽ dùng ở controller khi build url
      .build();
  }
}
