package com.stream.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/")
public class HomeController {
    

    @GetMapping
    public ResponseEntity<Map<String, Object>> home(){
        Map<String, Object> response = new HashMap<>();
        return ResponseEntity.ok().body(response);
    }
}
