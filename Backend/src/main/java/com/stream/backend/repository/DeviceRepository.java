package com.stream.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stream.backend.entity.Device;

public interface DeviceRepository extends JpaRepository<Device, Integer> {
    
}
