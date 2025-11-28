package com.stream.backend.service.implementation;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stream.backend.entity.Member;
import com.stream.backend.repository.MemberRepository;
import com.stream.backend.service.MemberService;;

@Service
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member login(String username, String password) {
        Optional<Member> opt = memberRepository.findByUsernameAndPassword(username, password);
        return opt.orElse(null);  // login sai trả về null 
    }
}
