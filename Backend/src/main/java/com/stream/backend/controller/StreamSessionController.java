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
@CrossOrigin(origins = "*")
public class StreamSessionController {

    private final StreamSessionService streamSessionService;
    private final FfmpegService ffmpegService;

    public StreamSessionController(StreamSessionService streamSessionService, FfmpegService ffmpegService) {
        this.streamSessionService = streamSessionService;
        this.ffmpegService = ffmpegService;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllStreamSessions(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        var pageData = streamSessionService.getAllStreamSessions(userId, page, size, sort);

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

    // GET /api/stream-sessions/status-map?streamIds=1,2,3
    @GetMapping("/status-map")
    public ResponseEntity<Map<String, Object>> getStatusMap(@RequestParam("streamIds") String streamIds) {

        List<Integer> ids = Arrays.stream(streamIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.valueOf(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Integer, String> map = streamSessionService.getStatusMapByStreamIds(ids);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Status map fetched successfully");
        response.put("statusMap", map);
        response.put("count", map.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stream/{streamId}")
    public ResponseEntity<Map<String, Object>> getStreamSessionByStreamId(
            @PathVariable("streamId") Integer streamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

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

    @PostMapping("/start/{streamId}")
    public ResponseEntity<Map<String, Object>> startStreamSession(@PathVariable("streamId") Integer streamId) {
        Map<String, Object> response = new HashMap<>();
        try {
            StreamSession started = streamSessionService.startSessionForStream(streamId);

            response.put("message", "StreamSession started successfully");
            response.put("streamSession", started);

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

    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> adminGetAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        var pageData = streamSessionService.adminGetAll(status, page, size, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin StreamSessions fetched successfully");
        response.put("streamSessions", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllStreamSessionsList(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "id,desc") String sort) {

        var list = streamSessionService.getAllStreamSessionsList(userId, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "StreamSessions fetched successfully");
        response.put("streamSessions", list);
        response.put("totalElements", list.size());

        return ResponseEntity.ok(response);
    }

}
