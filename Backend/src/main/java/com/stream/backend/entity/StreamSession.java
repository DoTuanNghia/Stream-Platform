package com.stream.backend.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tblstreamsession")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String specification;

    /*
     * 1. SCHEDULED: Có lịch, chưa chạy Ffmpeg
     * 2. ACTIVE: Đang chạy Ffmpeg
     * 3. STOPPED: Đã dừng Ffmpeg
     */

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    // 1 Device có N StreamSession
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties("streamSessions")
    private Device device;

    // 1 Stream có 1 StreamSession (stream_id UNIQUE)
    @OneToOne
    @JoinColumn(name = "stream_id", nullable = false, unique = true)
    @JsonIgnoreProperties({ "streamSession", "channel" })
    private Stream stream;
}
