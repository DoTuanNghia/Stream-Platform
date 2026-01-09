package com.stream.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbladmin")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends Member {

}
