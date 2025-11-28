package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stream.backend.entity.Channel;
import com.stream.backend.entity.Stream;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.service.StreamService;

@Service
public class StreamServiceImpl implements StreamService{
    private final StreamRepository streamRepository;
    private final ChannelRepository channelRepository;
    public StreamServiceImpl(StreamRepository streamRepository, ChannelRepository channelRepository){ 
        this.streamRepository = streamRepository;
        this.channelRepository = channelRepository;
    }

    @Override
    public List<Stream> getStreamsByChannelId(Integer channelId){
        return streamRepository.findByChannelId(channelId);
    }

    @Override
    public Stream createStream(Stream stream, Integer channelId){
        Channel channel = channelRepository.findById(channelId)
                .orElse(null);
        stream.setChannel(channel);
        return streamRepository.save(stream);
    }
}
