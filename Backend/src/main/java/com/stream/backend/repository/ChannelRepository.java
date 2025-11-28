package com.stream.backend.repository;

import com.stream.backend.entity.Channel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {
    List<Channel> findAll();
    List<Channel> findByUserId(Integer userId);
}
