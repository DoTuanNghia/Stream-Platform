package com.stream.backend.service;

import com.stream.backend.entity.Member;

import org.springframework.stereotype.Service;

public interface MemberService {
    public Member login(String username, String password);
}
