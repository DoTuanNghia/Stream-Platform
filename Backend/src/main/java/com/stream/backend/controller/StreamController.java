package com.stream.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.stream.backend.entity.Stream;
import com.stream.backend.service.StreamService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/streams")
@CrossOrigin(origins = "*")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    public static record BulkCreateItem(String name, String keyStream) {
    }

    public static record BulkCreateRequest(List<BulkCreateItem> items) {
    }

    private Integer resolveUserId(Integer userIdParam, HttpServletRequest request) {
        if (userIdParam != null)
            return userIdParam;

        // fallback: header X-USER-ID (nếu bạn đang dùng kiểu này)
        try {
            String h = request.getHeader("X-USER-ID");
            if (h != null && !h.isBlank())
                return Integer.valueOf(h.trim());
        } catch (Exception ignore) {
        }

        return null;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getStreamsByUserId(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            HttpServletRequest request) {

        Integer uid = resolveUserId(userId, request);
        if (uid == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "Missing userId (query param) or X-USER-ID header");
            return ResponseEntity.badRequest().body(err);
        }

        var pageData = streamService.getStreamsByUserId(uid, page, size, sort);

        Map<String, Object> res = new HashMap<>();
        res.put("streams", pageData.getContent());
        res.put("page", pageData.getNumber());
        res.put("size", pageData.getSize());
        res.put("totalElements", pageData.getTotalElements());
        res.put("totalPages", pageData.getTotalPages());
        res.put("last", pageData.isLast());

        return ResponseEntity.ok(res);
    }

    @PostMapping("")
    public ResponseEntity<Object> createStream(
            @RequestParam(required = false) Integer userId,
            @RequestBody Stream stream,
            HttpServletRequest request) {

        Integer uid = resolveUserId(userId, request);
        if (uid == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "Missing userId (query param) or X-USER-ID header");
            return ResponseEntity.badRequest().body(err);
        }

        var saved = streamService.createStream(stream, uid);
        return ResponseEntity.status(201).body(saved);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Object> createStreamsBulk(
            @RequestParam(required = false) Integer userId,
            @RequestBody BulkCreateRequest body,
            HttpServletRequest request) {

        Integer uid = resolveUserId(userId, request);
        if (uid == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "Missing userId (query param) or X-USER-ID header");
            return ResponseEntity.badRequest().body(err);
        }

        List<BulkCreateItem> items = (body == null) ? null : body.items();
        if (items == null || items.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "items is required");
            return ResponseEntity.badRequest().body(err);
        }

        List<Stream> streams = new ArrayList<>();
        for (BulkCreateItem it : items) {
            if (it == null)
                continue;
            Stream s = new Stream();
            s.setName(it.name());
            s.setKeyStream(it.keyStream());
            streams.add(s);
        }

        var saved = streamService.createStreamsBulk(streams, uid);
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
    public ResponseEntity<Object> updateStream(
            @PathVariable("streamId") Integer streamId,
            @RequestBody Stream stream) {

        Stream updated = streamService.updateStream(streamId, stream);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllStreamsByUserId(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "name,asc") String sort,
            HttpServletRequest request) {

        Integer uid = resolveUserId(userId, request);
        if (uid == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "Missing userId (query param) or X-USER-ID header");
            return ResponseEntity.badRequest().body(err);
        }

        var list = streamService.getStreamsByUserId(uid, sort);

        Map<String, Object> res = new HashMap<>();
        res.put("streams", list);
        res.put("totalElements", list.size());
        return ResponseEntity.ok(res);
    }

}
