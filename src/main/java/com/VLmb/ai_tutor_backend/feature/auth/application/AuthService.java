package com.VLmb.ai_tutor_backend.feature.auth.application;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenRequest;
import com.VLmb.ai_tutor_backend.feature.auth.domain.RefreshToken;
import com.VLmb.ai_tutor_backend.feature.auth.domain.Role;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserRepository userRepository,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    public void register(RegisterRequest request) throws IllegalStateException {
        log.info("event=auth_register_start user_name={}", request.userName());

        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new DuplicateResourceException("User", "userName", request.userName());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User newUser = new User();
        newUser.setUserName(request.userName());
        newUser.setEmail(request.email());
        newUser.setEnabled(true);
        newUser.setRoles(Set.of(Role.USER));
        newUser.setHashPassword(passwordEncoder.encode(request.password()));

        userRepository.save(newUser);
        log.info("event=auth_register_success user_id={} user_name={}", newUser.getId(), newUser.getUserName());

    }

    public LoginResponse login(LoginRequest request) {
        log.info("event=auth_login_start user_name={}", request.userName());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password()
                )
        );

        UserDetails principal = (UserDetails) auth.getPrincipal();
        String access = jwtService.generateAccessToken(principal);
        RefreshToken refresh = refreshTokenService.createRefreshToken(principal.getUsername());
        log.info("event=auth_login_success user_name={}", principal.getUsername());
        return new LoginResponse(access, refresh.getToken());
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    log.info("event=auth_refresh_start user_id={} user_name={}", user.getId(), user.getUserName());
                    var authorities = user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())) // если enum
                            .toList();

                    UserDetails principal = new CustomUserDetails(user, authorities);

                    refreshTokenService.deleteByUserId(user.getId());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUserName());
                    String newAccessToken = jwtService.generateAccessToken(principal);

                    log.info("event=auth_refresh_success user_id={} user_name={}", user.getId(), user.getUserName());
                    return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(Long id) {

    refreshTokenService.deleteByUserId(id);
    log.info("event=auth_logout user_id={}", id);

    }

}
