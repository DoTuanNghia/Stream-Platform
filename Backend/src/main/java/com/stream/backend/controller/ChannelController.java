package com.stream.backend.controller;

import java.util.HashMap;
import java.util.Map;

import com.stream.backend.entity.Channel;
import com.stream.backend.service.ChannelService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels")
@CrossOrigin(origins = "http://localhost:5173")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Page<Channel> pageData = channelService.getAllChannels(page, size, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Channels fetched successfully");
        response.put("channels", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getChannelsByUserId(
            @PathVariable("userId") Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Page<Channel> pageData = channelService.getChannelsByUserId(userId, page, size, sort);

        Map<String, Object> response = new HashMap<>();
        if (pageData.isEmpty()) {
            response.put("message", "No channels found for user");
            response.put("userId", userId);
            response.put("channels", pageData.getContent());
            response.put("page", pageData.getNumber());
            response.put("size", pageData.getSize());
            response.put("totalElements", pageData.getTotalElements());
            response.put("totalPages", pageData.getTotalPages());
            response.put("last", pageData.isLast());
            return ResponseEntity.status(404).body(response);
        }

        response.put("message", "Channels fetched successfully");
        response.put("channels", pageData.getContent());
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("last", pageData.isLast());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createChannel(
            @PathVariable("userId") Integer userId,
            @RequestBody Channel channel) {
        var saved = channelService.createChannel(userId, channel);
        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Map<String, Object>> deleteChannel(@PathVariable("channelId") Integer channelId) {

        Map<String, Object> response = new HashMap<>();
        response.put("channelId", channelId);

        if (!channelService.existsChannel(channelId)) {
            response.put("message", "Channel not found");
            return ResponseEntity.status(404).body(response);
        }

        channelService.deleteChannel(channelId);

        response.put("message", "Channel deleted successfully");
        return ResponseEntity.ok(response);
    }
}
