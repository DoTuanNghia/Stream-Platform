package com.stream.backend.service;

import com.stream.backend.entity.Channel;
import org.springframework.data.domain.Page;

public interface ChannelService {

    Page<Channel> getAllChannels(int page, int size, String sort);

    Page<Channel> getChannelsByUserId(Integer userId, int page, int size, String sort);

    Channel createChannel(Integer userId, Channel channel);

    void deleteChannel(Integer channelId);

    boolean existsChannel(Integer channelId);
}
