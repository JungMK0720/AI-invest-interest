package com.finmate.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank String name) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    /** 응답에는 비밀번호 관련 필드를 절대 포함하지 않는다. */
    public record UserResponse(String id, String email, String name) {
    }

    public record LoginResponse(String accessToken, UserResponse user) {
    }
}
