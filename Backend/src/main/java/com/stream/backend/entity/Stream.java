package com.stream.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblstream")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "keyStream", nullable = false, length = 255)
    private String keyStream;

    @Column(name = "timeStart")
    private LocalDateTime timeStart;

    @Column
    private Integer duration;

    // 1 Channel có N Stream
    @ManyToOne
    @JoinColumn(name = "channel_id", nullable = false)
    @JsonBackReference   // đây là phía con
    private Channel channel;

    // StreamSession bạn có thể giữ nguyên
    @OneToOne(mappedBy = "stream")
    private StreamSession streamSession;
}
