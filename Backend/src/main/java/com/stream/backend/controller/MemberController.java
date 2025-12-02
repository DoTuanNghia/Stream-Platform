package com.stream.backend.controller;

import com.stream.backend.entity.Member;
import com.stream.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MemberController {

    private final MemberRepository memberRepository;

    // GET /api/members  -> trả danh sách tất cả member (admin + user)
    @GetMapping
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }
}
