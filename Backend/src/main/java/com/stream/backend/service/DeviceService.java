package com.stream.backend.service;

import java.util.List;

import com.stream.backend.entity.Device;

public interface DeviceService {
    List<Device> getAllDevices();

    Device createDevice(Device device);

    void deleteDevice(Integer id);

    List<Device> getAvailableDevices();
}
