package com.stream.backend.service;

import com.stream.backend.entity.Member;

public interface MemberService {
    public Member login(String username, String password);
}
