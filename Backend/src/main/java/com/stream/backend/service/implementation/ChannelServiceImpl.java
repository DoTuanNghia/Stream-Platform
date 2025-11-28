package com.stream.backend.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stream.backend.entity.Channel;
import com.stream.backend.entity.User;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.repository.UserRepository;
import com.stream.backend.service.ChannelService;

@Service
public class ChannelServiceImpl implements ChannelService {
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Channel> getAllChannels() {
        return channelRepository.findAll();
    }

    @Override
    public List<Channel> getChannelsByUserId(Integer userId) {
        return channelRepository.findByUserId(userId);
    }

    @Override
    public Channel createChannel(Integer userId, Channel channel) {
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        channel.setUser(user);

        return channelRepository.save(channel);
    }
}
