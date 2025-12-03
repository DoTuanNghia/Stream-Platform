package com.stream.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stream.backend.entity.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {

    @Query("SELECT d FROM Device d " +
           "WHERE d.currentSession < d.totalSession " +
           "ORDER BY d.id ASC")
    List<Device> findAvailableDevices();
}
