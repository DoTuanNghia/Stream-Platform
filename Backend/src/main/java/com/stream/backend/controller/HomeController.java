package com.stream.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stream.backend.service.ChannelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/")
public class HomeController {

    private final ChannelService channelService;

    public HomeController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
     public ResponseEntity<Map<String, Object>> home() {
        var channels = channelService.getAllChannels();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Stream Platform Backend!");
        response.put("channels", channels);

        return ResponseEntity.ok(response);
    }
}
