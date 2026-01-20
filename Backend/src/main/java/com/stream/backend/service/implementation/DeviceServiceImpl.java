package com.stream.backend.service.implementation;

import com.stream.backend.entity.Device;
import com.stream.backend.repository.DeviceRepository;
import com.stream.backend.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Override
    public List<Device> getAvailableDevices() {
        // lọc device còn slot (currentSession < totalSession), sort theo id tăng dần
        return deviceRepository.findAll().stream()
                .filter(d -> d.getCurrentSession() < d.getTotalSession())
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .collect(Collectors.toList());
    }
}
