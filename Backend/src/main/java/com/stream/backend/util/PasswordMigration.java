package com.stream.backend.util;

import com.stream.backend.entity.Member;
import com.stream.backend.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Script chạy một lần để migrate password từ plaintext sang BCrypt.
 * Script sẽ kiểm tra xem password đã được mã hóa chưa trước khi migrate.
 * 
 * HƯỚNG DẪN SỬ DỤNG:
 * 1. Chạy ứng dụng một lần để migrate tất cả password cũ
 * 2. Sau khi migrate xong, COMMENT hoặc XÓA annotation @Component để script
 * không chạy lại
 * 3. Hoặc đổi tên file thành PasswordMigration.java.bak
 */
@Component
@Order(1) // Chạy sớm nhất khi khởi động
public class PasswordMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigration.class);

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
            logger.info("Password migration is DISABLED. Skipping...");
            return;
        }

        logger.info("========================================");
        logger.info("BẮT ĐẦU MIGRATE PASSWORD SANG BCRYPT");
        logger.info("========================================");

        List<Member> allMembers = memberRepository.findAll();
        int migratedCount = 0;
        int skippedCount = 0;

        for (Member member : allMembers) {
            String currentPassword = member.getPassword();

            // Kiểm tra xem password đã được mã hóa BCrypt chưa
            if (isBCryptHash(currentPassword)) {
                logger.info("User '{}' (id={}) - Password đã được mã hóa, BỎ QUA",
                        member.getUsername(), member.getId());
                skippedCount++;
                continue;
            }

            // Mã hóa password plaintext
            String encodedPassword = passwordEncoder.encode(currentPassword);
            member.setPassword(encodedPassword);
            memberRepository.save(member);

            logger.info("User '{}' (id={}) - Đã migrate password thành công",
                    member.getUsername(), member.getId());
            migratedCount++;
        }

        logger.info("========================================");
        logger.info("KẾT QUẢ MIGRATE PASSWORD:");
        logger.info("- Tổng số user: {}", allMembers.size());
        logger.info("- Đã migrate: {}", migratedCount);
        logger.info("- Bỏ qua (đã mã hóa): {}", skippedCount);
        logger.info("========================================");
    }

    /**
     * Kiểm tra xem password có phải là BCrypt hash không
     */
    private boolean isBCryptHash(String password) {
        if (password == null || password.length() != 60) {
            return false;
        }
        // BCrypt hash format: $2a$XX$... hoặc $2b$XX$... hoặc $2y$XX$...
        return password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
