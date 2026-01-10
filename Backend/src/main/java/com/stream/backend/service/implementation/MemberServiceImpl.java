package com.stream.backend.service.implementation;

import com.stream.backend.entity.Member;
import com.stream.backend.entity.Role;
import com.stream.backend.entity.User;
import com.stream.backend.repository.MemberRepository;
import com.stream.backend.repository.UserRepository;
import com.stream.backend.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public MemberServiceImpl(MemberRepository memberRepository, UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Member login(String username, String password) {
        Optional<Member> opt = memberRepository.findByUsernameAndPassword(username, password);
        return opt.orElse(null);
    }

    @Override
    @Transactional
    public Member createMember(String fullName, String username, String password, Role role) {
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Tên người dùng không được để trống");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được để trống");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password không được để trống");

        Role finalRole = (role == null) ? Role.USER : role;

        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        // Hiện tại hệ thống bạn có bảng tbluser, nên tạo User cho cả USER/ADMIN
        // (để giữ JOINED inheritance đồng nhất). Role sẽ phân biệt quyền.
        User u = new User();
        u.setName(fullName); // FE fullName -> DB name
        u.setUsername(username);
        u.setPassword(password); // đang plaintext theo hệ thống hiện tại
        u.setRole(finalRole);

        return userRepository.save(u);
    }

    @Override
    @Transactional
    public Member updateMember(Integer id, String fullName, String password, Role role) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy member id=" + id));

        if (fullName != null && !fullName.isBlank()) {
            u.setName(fullName);
        }
        if (password != null && !password.isBlank()) {
            u.setPassword(password);
        }
        if (role != null) {
            u.setRole(role);
        }

        return userRepository.save(u);
    }

    @Override
    @Transactional
    public void deleteMember(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy member id=" + id);
        }
        userRepository.deleteById(id);
    }
}
