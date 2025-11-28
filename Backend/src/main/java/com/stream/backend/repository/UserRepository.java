package com.stream.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stream.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
}
