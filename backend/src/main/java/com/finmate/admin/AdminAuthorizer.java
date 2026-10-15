package com.finmate.admin;

import com.finmate.common.ApiException;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import com.finmate.user.UserRole;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizer {

    private final UserRepository users;

    public AdminAuthorizer(UserRepository users) {
        this.users = users;
    }

    public void requireAdmin(String userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("로그인이 필요합니다."));
        if (user.getRole() != UserRole.ADMIN) {
            throw ApiException.forbidden("관리자 권한이 필요합니다.");
        }
    }
}
