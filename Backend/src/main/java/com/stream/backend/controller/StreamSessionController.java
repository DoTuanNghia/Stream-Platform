package com.stream.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stream.backend.entity.StreamSession;
import com.stream.backend.service.StreamSessionService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/stream-sessions")
@CrossOrigin(origins = "http://localhost:5173")
public class StreamSessionController {
    private final StreamSessionService streamSessionService;

    public StreamSessionController(StreamSessionService streamSessionService) {
        this.streamSessionService = streamSessionService;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllStreamSessions() {
        var streamSessions = streamSessionService.getAllStreamSessions();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Stream Platform Backend!");
        response.put("streamSessions", streamSessions);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> getStreamSessionByDeviceId(@PathVariable("deviceId") Integer deviceId) {
        var streamSessions = streamSessionService.getStreamSessionsByDeviceId(deviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Stream Platform Backend!");
        response.put("streamSessions", streamSessions);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stream/{streamId}")
    public ResponseEntity<Map<String, Object>> getStreamSessionByStreamId(@PathVariable("streamId") Integer streamId) {
        var streamSessions = streamSessionService.getStreamSessionsByStreamId(streamId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Stream Platform Backend!");
        response.put("streamSessions", streamSessions);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/device/{deviceId}/stream/{streamId}")
    public ResponseEntity<Map<String, Object>> createStreamSession(
            @PathVariable("deviceId") Integer deviceId,
            @PathVariable("streamId") Integer streamId,
            @RequestBody StreamSession streamSession) {

        var saved = streamSessionService.createStreamSession(streamSession, deviceId, streamId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSession created successfully");
        response.put("streamSession", saved);

        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/start/{streamId}")
    public ResponseEntity<Map<String, Object>> startStreamSession(
            @PathVariable("streamId") Integer streamId) {

        Map<String, Object> response = new HashMap<>();
        try {
            StreamSession started = streamSessionService.startSessionForStream(streamId);

            response.put("message", "StreamSession started successfully");
            response.put("streamSession", started);
            response.put("deviceId", started.getDevice().getId());
            response.put("deviceName", started.getDevice().getName());

            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{streamSessionId}")
    public ResponseEntity<Map<String, Object>> stopStreamSession(
            @PathVariable("streamSessionId") Integer streamSessionId) {

        StreamSession streamSession = streamSessionService.getStreamSessionById(streamSessionId);
        var stopped = streamSessionService.stopStreamSession(streamSession);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSession stopped successfully");
        response.put("streamSession", stopped);

        return ResponseEntity.ok(response);
    }
}
