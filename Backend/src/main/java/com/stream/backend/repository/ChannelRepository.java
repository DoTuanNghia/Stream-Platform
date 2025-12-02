package com.stream.backend.repository;

import com.stream.backend.entity.Channel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {
    List<Channel> findAll();
    List<Channel> findByUserId(Integer userId);
    Channel save(Channel channel);
    void delete(Channel channel);
}
