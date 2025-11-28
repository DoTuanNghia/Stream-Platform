package com.stream.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tblchannel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "channel_code", nullable = false, length = 255)
    private String channelCode;

    // 1 User có N Channel
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // 1 Channel có N Stream
    @OneToMany(mappedBy = "channel")
    @JsonIgnore
    private List<Stream> streams;
}
