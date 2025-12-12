package com.stream.backend.youtube;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;

@Configuration
public class YouTubeClientConfig {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${youtube.applicationName:StreamPlatform}")
    private String applicationName;

    @Value("${youtube.credentials.path}")
    private String credentialsPath;  // vd: classpath:youtube-client-secret.json

    @Value("${youtube.tokens.dir:tokens}")
    private String tokensDir;

    private static final String YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube";

    @Bean
    public YouTube youtube() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport);

        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    private Credential getCredentials(NetHttpTransport httpTransport) throws IOException {
        // Bỏ "classpath:" ra, chỉ lấy tên file
        String resourceName = credentialsPath.replace("classpath:", "");
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (in == null) {
            throw new IOException("Không tìm thấy file credentials: " + credentialsPath);
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singletonList(YOUTUBE_SCOPE))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDir)))
                .setAccessType("offline")
                .build();

        // LocalServerReceiver sẽ mở browser ở http://localhost:8888 để login
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
