package com.stream.backend.service;

import com.stream.backend.entity.Channel;
import java.util.List;

public interface ChannelService {
    public List<Channel> getAllChannels();
    public List<Channel> getChannelsByUserId(Integer userId);
}
