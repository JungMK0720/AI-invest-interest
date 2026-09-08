package com.finmate.auth;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 로그인 토큰 저장소.
 *
 * 1차에서는 프로세스 메모리에만 보관한다. 서버를 재시작하면 토큰이 사라지며,
 * 다중 인스턴스로 확장하면 공유되지 않는다. JWT 또는 외부 세션 저장소는 2차 과제다 (ADR-009).
 */
@Component
public class AuthTokenStore {

    private final Map<String, String> tokenToUserId = new ConcurrentHashMap<>();

    public String issue(String userId) {
        String token = UUID.randomUUID().toString();
        tokenToUserId.put(token, userId);
        return token;
    }

    public Optional<String> resolveUserId(String token) {
        return Optional.ofNullable(tokenToUserId.get(token));
    }
}
