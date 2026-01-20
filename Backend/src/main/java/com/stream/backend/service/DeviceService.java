package com.stream.backend.service;

import java.util.List;

import com.stream.backend.entity.Device;

public interface DeviceService {
    List<Device> getAllDevices();

    List<Device> getAvailableDevices();
}
