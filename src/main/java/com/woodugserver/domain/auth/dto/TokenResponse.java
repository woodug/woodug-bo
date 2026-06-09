package com.woodugserver.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
