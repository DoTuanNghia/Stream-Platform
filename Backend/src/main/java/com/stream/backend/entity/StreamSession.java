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

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @OneToOne
    @JoinColumn(name = "stream_id", nullable = false, unique = true)
    @JsonIgnoreProperties({ "streamSession" })
    private Stream stream;
}
