package com.stream.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Column(nullable = false, length = 50)
    private String status;

    // 1 Device có N StreamSession
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnore
    private Device device;

    // 1 Stream có 1 StreamSession (stream_id UNIQUE)
    @OneToOne
    @JoinColumn(name = "stream_id", nullable = false, unique = true)
    @JsonIgnore
    private Stream stream;
}
