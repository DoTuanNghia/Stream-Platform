package com.stream.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbluser")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class User extends Member {
}
