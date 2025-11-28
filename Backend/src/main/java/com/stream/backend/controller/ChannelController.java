package com.stream.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stream.backend.entity.Channel;
import com.stream.backend.service.ChannelService;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {
    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAllChannels() {
        var channels = channelService.getAllChannels();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Stream Platform Backend!");
        response.put("channels", channels);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getChannelsByUserId(@PathVariable("userId") Integer userId) {
        var channels = channelService.getChannelsByUserId(userId);

        Map<String, Object> response = new HashMap<>();

        if (channels == null || channels.isEmpty()) {
            response.put("message", "No channels found for user");
            response.put("userId", userId);
            return ResponseEntity.status(404).body(response);
        }

        response.put("message", "Channels fetched successfully");
        response.put("channels", channels);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createChannel(
            @PathVariable("userId") Integer userId,
            @RequestBody Channel channel) {

        var saved = channelService.createChannel(userId, channel);
        return ResponseEntity.status(201).body(saved);
    }

}
