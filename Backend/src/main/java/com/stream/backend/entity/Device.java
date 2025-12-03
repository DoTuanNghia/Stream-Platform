package com.stream.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tbldevice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "current_session", nullable = false)
    private Integer currentSession = 0;     // số session đang chạy trên máy

    @Column(name = "total_session", nullable = false)
    private Integer totalSession = 10;       // tối đa session cho phép

    @OneToMany(mappedBy = "device")
    @JsonIgnore
    private List<StreamSession> streamSessions;   // 1 Device có N StreamSession
}
