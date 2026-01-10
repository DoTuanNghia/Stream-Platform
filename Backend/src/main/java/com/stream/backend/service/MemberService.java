package com.stream.backend.service;

import com.stream.backend.entity.Member;
import com.stream.backend.entity.Role;

public interface MemberService {
    public Member login(String username, String password);

    Member createMember(String fullName, String username, String password, Role role);

    Member updateMember(Integer id, String fullName, String password, Role role);

    void deleteMember(Integer id);
}
