package com.VLmb.ai_tutor_backend.repository;

import com.VLmb.ai_tutor_backend.entity.RefreshToken;
import com.VLmb.ai_tutor_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);
}