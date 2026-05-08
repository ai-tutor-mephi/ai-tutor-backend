package com.VLmb.ai_tutor_backend.feature.auth.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.RefreshToken;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TokenRefreshException;
import com.VLmb.ai_tutor_backend.feature.auth.infra.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import com.VLmb.ai_tutor_backend.shared.security.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Пользователь с именем '" + username + "' не найден."));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Токен обновления истек. Выполните вход заново.");
        }

        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        return refreshTokenRepository.deleteByUser(user);
    }
}
