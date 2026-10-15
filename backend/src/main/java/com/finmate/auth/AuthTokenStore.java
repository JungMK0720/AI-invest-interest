package com.finmate.auth;

import com.finmate.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 영구 DB에 해시만 보관하는 만료·폐기 가능한 Bearer 세션. */
@Component
public class AuthTokenStore {

    public record IssuedToken(String value, Instant expiresAt) { }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthSessionRepository sessions;
    private final long sessionDays;

    public AuthTokenStore(AuthSessionRepository sessions,
                          @Value("${finmate.auth.session-days:30}") long sessionDays) {
        if (sessionDays < 1 || sessionDays > 365) {
            throw new IllegalArgumentException("세션 유효기간은 1~365일이어야 합니다.");
        }
        this.sessions = sessions;
        this.sessionDays = sessionDays;
    }

    @Transactional
    public IssuedToken issue(String userId) {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(sessionDays, ChronoUnit.DAYS);
        sessions.save(new AuthSession(hash(token), userId, now, expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sessions.findById(hash(token))
                .filter(session -> session.isActiveAt(Instant.now()))
                .map(AuthSession::getUserId);
    }

    /** 공개 학습 API는 무인증 제출을 허용하지만, 보내온 토큰이 틀리면 저장을 건너뛰지 않는다. */
    public Optional<String> resolveOptionalBearerUserId(String authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        if (!authorization.startsWith("Bearer ")) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }
        return Optional.of(resolveUserId(authorization.substring("Bearer ".length()).trim())
                .orElseThrow(() -> ApiException.unauthorized("로그인이 필요합니다.")));
    }

    @Transactional
    public boolean revoke(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<AuthSession> found = sessions.findById(hash(token))
                .filter(session -> session.isActiveAt(Instant.now()));
        found.ifPresent(session -> session.revoke(Instant.now()));
        return found.isPresent();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
