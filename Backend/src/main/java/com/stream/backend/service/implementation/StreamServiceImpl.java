package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stream.backend.entity.Stream;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.service.StreamService;

@Service
public class StreamServiceImpl implements StreamService{
    private final StreamRepository streamRepository;
    public StreamServiceImpl(StreamRepository streamRepository) {
        this.streamRepository = streamRepository;
    }

    public List<Stream> getStreamsByChannelId(Integer channelId){
        return streamRepository.findByChannelId(channelId);
    }
}
