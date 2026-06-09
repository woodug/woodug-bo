package com.woodugserver.domain.auth.service;

import com.woodugserver.domain.auth.dto.*;
import com.woodugserver.domain.user.entity.User;
import com.woodugserver.domain.user.entity.UserStatus;
import com.woodugserver.domain.user.repository.UserRepository;
import com.woodugserver.global.exception.CustomException;
import com.woodugserver.global.exception.ErrorCode;
import com.woodugserver.global.jwt.JwtProvider;
import com.woodugserver.global.redis.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        checkUserStatus(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse reissue(ReissueRequest request) {
        Long userId = refreshTokenStore.getUserId(request.refreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        checkUserStatus(user);
        return issueTokens(user);
    }

    public void logout(Long userId) {
        refreshTokenStore.deleteByUserId(userId);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        Duration ttl = Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis());

        refreshTokenStore.save(user.getId(), refreshToken, ttl);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.SUSPENDED_USER);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }
    }
}
