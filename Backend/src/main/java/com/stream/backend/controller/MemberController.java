package com.stream.backend.controller;

import com.stream.backend.entity.Member;
import com.stream.backend.entity.Role;
import com.stream.backend.repository.MemberRepository;
import com.stream.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @GetMapping
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // FE gửi: { fullName, username, password, role } role optional
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String fullName = body.get("fullName") == null ? null : body.get("fullName").toString();
            String username = body.get("username") == null ? null : body.get("username").toString();
            String password = body.get("password") == null ? null : body.get("password").toString();

            Role role = null;
            if (body.get("role") != null) {
                role = Role.valueOf(body.get("role").toString().toUpperCase());
            }

            Member created = memberService.createMember(fullName, username, password, role);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            String fullName = body.get("fullName") == null ? null : body.get("fullName").toString();
            String password = body.get("password") == null ? null : body.get("password").toString();

            Role role = null;
            if (body.get("role") != null) {
                role = Role.valueOf(body.get("role").toString().toUpperCase());
            }

            Member updated = memberService.updateMember(id, fullName, password, role);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            memberService.deleteMember(id);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return memberRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Không tìm thấy member id=" + id));
    }

}
