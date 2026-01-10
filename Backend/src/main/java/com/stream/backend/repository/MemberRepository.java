package com.stream.backend.repository;

import com.stream.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    Optional<Member> findByUsernameAndPassword(String username, String password);

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);
}
