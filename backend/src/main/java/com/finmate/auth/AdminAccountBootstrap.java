package com.finmate.auth;

import com.finmate.user.User;
import com.finmate.user.UserRepository;
import com.finmate.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 자격은 공개 가입 요청이 아닌 서버 환경 변수에서만 부여한다. */
@Component
public class AdminAccountBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final String email;
    private final String password;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminAccountBootstrap(UserRepository users,
            @Value("${finmate.admin.bootstrap-email:}") String email,
            @Value("${finmate.admin.bootstrap-password:}") String password) {
        this.users = users;
        this.email = email.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() && password.isEmpty()) {
            return;
        }
        if (email.isEmpty() || password.length() < 12) {
            throw new IllegalStateException("관리자 초기 계정에는 이메일과 12자 이상의 비밀번호가 필요합니다.");
        }
        User existing = users.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("관리자 초기 이메일이 기존 일반 계정과 충돌합니다.");
            }
            return;
        }
        User admin = new User(UUID.randomUUID().toString(), email, encoder.encode(password), "운영자", Instant.now());
        admin.promoteToAdmin();
        users.save(admin);
    }
}
