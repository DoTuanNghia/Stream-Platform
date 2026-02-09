package com.stream.backend.util;

import com.stream.backend.entity.Member;
import com.stream.backend.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1) 
public class PasswordMigration implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // Flag để bật/tắt migration (đặt false sau khi đã migrate xong)
    private static final boolean ENABLE_MIGRATION = true;

    public PasswordMigration(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!ENABLE_MIGRATION) {
            return;
        }

        List<Member> allMembers = memberRepository.findAll();

        for (Member member : allMembers) {
            String currentPassword = member.getPassword();

            // Kiểm tra xem password đã được mã hóa BCrypt chưa
            if (isBCryptHash(currentPassword)) {
                continue;
            }

            // Mã hóa password plaintext
            String encodedPassword = passwordEncoder.encode(currentPassword);
            member.setPassword(encodedPassword);
            memberRepository.save(member);
        }
    }

    private boolean isBCryptHash(String password) {
        if (password == null || password.length() != 60) {
            return false;
        }
        return password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
