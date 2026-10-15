package com.finmate.auth;

import com.finmate.common.ApiException;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, AuthTokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    @Transactional
    public AuthDtos.UserResponse signup(AuthDtos.SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("이미 가입된 이메일입니다.");
        }
        User user = new User(
                UUID.randomUUID().toString(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                Instant.now());
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                // 계정 존재 여부를 노출하지 않도록 두 경우 모두 같은 메시지를 쓴다.
                .orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다."));
        AuthTokenStore.IssuedToken issued = tokenStore.issue(user.getId());
        return new AuthDtos.LoginResponse(issued.value(), issued.expiresAt(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(String userId) {
        return toResponse(userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("로그인이 필요합니다.")));
    }

    public void logout(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !tokenStore.revoke(authorization.substring("Bearer ".length()).trim())) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }
    }

    private AuthDtos.UserResponse toResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }
}
