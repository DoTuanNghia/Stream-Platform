package com.stream.backend.service.implementation;

import com.stream.backend.entity.Channel;
import com.stream.backend.entity.User;
import com.stream.backend.repository.ChannelRepository;
import com.stream.backend.repository.UserRepository;
import com.stream.backend.service.ChannelService;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<Channel> getAllChannels(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return channelRepository.findAll(pageable);
    }

    @Override
    public Page<Channel> getChannelsByUserId(Integer userId, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return channelRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional
    public Channel createChannel(Integer userId, Channel channel) {
        // Nên chặn trường hợp user không tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        channel.setUser(user);
        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Integer channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new RuntimeException("Channel not found");
        }
        channelRepository.deleteById(channelId);
    }

    @Override
    public boolean existsChannel(Integer channelId) {
        return channelRepository.existsById(channelId);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sortObj = parseSort(sort);
        return PageRequest.of(safePage, safeSize, sortObj);
    }

    private Sort parseSort(String sort) {
        // format: "id,desc" | "name,asc" | "channelCode,asc"
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;

            // Sort theo tên FIELD Java, không theo tên cột DB
            if (!isAllowedSortField(field)) field = "id";

            return Sort.by(dir, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
    }

    private boolean isAllowedSortField(String field) {
        return "id".equals(field)
                || "name".equals(field)
                || "channelCode".equals(field);
    }
}
