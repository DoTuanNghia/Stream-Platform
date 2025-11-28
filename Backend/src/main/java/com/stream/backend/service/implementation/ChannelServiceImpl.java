package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stream.backend.entity.Channel;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.service.ChannelService;

@Service
public class ChannelServiceImpl implements ChannelService{
    private final ChannelRepository channelRepository;

    public ChannelServiceImpl(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public List<Channel> getAllChannels(){
        return channelRepository.findAll();
    }

    @Override
    public List<Channel> getChannelsByUserId(Integer userId){
        return channelRepository.findByUserId(userId);
    }
}
