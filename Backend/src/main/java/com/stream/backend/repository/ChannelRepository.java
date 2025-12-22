package com.stream.backend.repository;

import com.stream.backend.entity.Channel;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {
    Channel save(Channel channel);

    void delete(Channel channel);

    Page<Channel> findAll(Pageable pageable);

    Page<Channel> findByUserId(Integer userId, Pageable pageable);

    boolean existsById(Integer id);
}
