package com.stream.backend.service;

import java.util.List;

import com.stream.backend.entity.Stream;

public interface StreamService {
    List<Stream> getStreamsByChannelId(Integer channelId);
} 
