package com.stream.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.stream.backend.entity.Stream;
import com.stream.backend.service.StreamService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/streams")
@CrossOrigin(origins = {
  "http://localhost:5173",
  "https://stream-platform-eta.vercel.app"
})

public class StreamController {
    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping("/channel/{channelId}")
    public ResponseEntity<Map<String, Object>> getStreamsByChannelId(
            @PathVariable("channelId") Integer channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timeStart,desc") String sort) {
        var pageData = streamService.getStreamsByChannelId(channelId, page, size, sort);

        Map<String, Object> response = new HashMap<>();

        if (pageData.isEmpty()) {
            response.put("message", "No stream found for channel");
            response.put("channelId", channelId);
            response.put("streams", pageData.getContent());
            response.put("page", pageData.getNumber());
            response.put("size", pageData.getSize());
            response.put("totalElements", pageData.getTotalElements());
            response.put("totalPages", pageData.getTotalPages());
            response.put("last", pageData.isLast());
            return ResponseEntity.status(404).body(response);
        }

        response.put("message", "Streams fetched successfully");
        response.put("streams", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/channel/{channelId}")
    public ResponseEntity<Object> createStream(
            @PathVariable("channelId") Integer channelId,
            @RequestBody Stream stream) {

        var saved = streamService.createStream(stream, channelId);

        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{streamId}")
    public ResponseEntity<Map<String, String>> deleteStream(@PathVariable("streamId") Integer streamId) {
        Stream stream = new Stream();
        stream.setId(streamId);
        streamService.deleteStream(stream);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Stream deleted successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{streamId}")
    public ResponseEntity<Object> updateStream(@PathVariable("streamId") Integer streamId, @RequestBody Stream stream) {
        Stream updated = streamService.updateStream(streamId, stream);
        return ResponseEntity.ok(updated);
    }
}
