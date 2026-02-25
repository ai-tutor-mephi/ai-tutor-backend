package com.VLmb.ai_tutor_backend.feature.auth.infra;

import com.VLmb.ai_tutor_backend.feature.auth.domain.RefreshToken;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);
}