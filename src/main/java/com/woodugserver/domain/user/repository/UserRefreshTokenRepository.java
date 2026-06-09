package com.woodugserver.domain.user.repository;

import com.woodugserver.domain.user.entity.User;
import com.woodugserver.domain.user.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByToken(String token);

    Optional<UserRefreshToken> findByUser(User user);

    void deleteByUser(User user);
}
