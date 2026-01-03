package com.stream.backend.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stream.backend.entity.StreamSession;
import com.stream.backend.service.StreamSessionService;
import com.stream.backend.service.FfmpegService;

@RestController
@RequestMapping("/api/stream-sessions")
@CrossOrigin(origins = {
  "http://localhost:5173",
  "https://stream-platform-eta.vercel.app"
})

public class StreamSessionController {

    private final StreamSessionService streamSessionService;
    private final FfmpegService ffmpegService;

    public StreamSessionController(StreamSessionService streamSessionService, FfmpegService ffmpegService) {
        this.streamSessionService = streamSessionService;
        this.ffmpegService = ffmpegService;
    }

    // GET /api/stream-sessions?page=0&size=10&sort=id,desc
    // ==> CHỈ ACTIVE (paging)
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllStreamSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        var pageData = streamSessionService.getAllStreamSessions(page, size, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSessions fetched successfully");
        response.put("streamSessions", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    // NEW: status map cho trang Stream
    // GET /api/stream-sessions/status-map?streamIds=1,2,3
    @GetMapping("/status-map")
    public ResponseEntity<Map<String, Object>> getStatusMap(@RequestParam("streamIds") String streamIds) {

        List<Integer> ids = Arrays.stream(streamIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        Map<Integer, String> map = streamSessionService.getStatusMapByStreamIds(ids);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Status map fetched successfully");
        response.put("statusMap", map);
        response.put("count", map.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> getStreamSessionByDeviceId(
            @PathVariable("deviceId") Integer deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        var pageData = streamSessionService.getStreamSessionsByDeviceId(deviceId, page, size, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSessions fetched successfully");
        response.put("deviceId", deviceId);
        response.put("streamSessions", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stream/{streamId}")
    public ResponseEntity<Map<String, Object>> getStreamSessionByStreamId(
            @PathVariable("streamId") Integer streamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        var pageData = streamSessionService.getStreamSessionsByStreamId(streamId, page, size, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSessions fetched successfully");
        response.put("streamId", streamId);
        response.put("streamSessions", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ffmpeg-stat/{streamKey}")
    public ResponseEntity<Map<String, Object>> getFfmpegStat(@PathVariable("streamKey") String streamKey) {
        Map<String, Object> response = new HashMap<>();
        response.put("streamKey", streamKey);
        response.put("stat", ffmpegService.getLatestStat(streamKey));
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
    public ResponseEntity<Map<String, Object>> startStreamSession(@PathVariable("streamId") Integer streamId) {
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
    public ResponseEntity<Map<String, Object>> stopStreamSession(@PathVariable("streamSessionId") Integer streamSessionId) {

        StreamSession streamSession = streamSessionService.getStreamSessionById(streamSessionId);
        var stopped = streamSessionService.stopStreamSession(streamSession);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSession stopped successfully");
        response.put("streamSession", stopped);

        return ResponseEntity.ok(response);
    }
}
