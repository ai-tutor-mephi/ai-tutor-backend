package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.AuthResponse;
import com.VLmb.ai_tutor_backend.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.dto.RegisterUserRequest;
import com.VLmb.ai_tutor_backend.dto.TokenRefreshRequest;
import com.VLmb.ai_tutor_backend.entity.RefreshToken;
import com.VLmb.ai_tutor_backend.entity.Role;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.exception.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
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

    public void register(RegisterUserRequest request) throws IllegalStateException {

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

    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password()
                )
        );

        UserDetails principal = (UserDetails) auth.getPrincipal();
        String access = jwtService.generateAccessToken(principal);
        RefreshToken refresh = refreshTokenService.createRefreshToken(principal.getUsername());
        return new AuthResponse(access, refresh.getToken());
    }

    public AuthResponse refresh(TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails principal = new CustomUserDetails(user);

                    refreshTokenService.deleteByUserId(user.getId());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUserName());
                    String newAccessToken = jwtService.generateAccessToken(principal);

                    return new AuthResponse(newAccessToken, newRefreshToken.getToken());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(Long id) {

    refreshTokenService.deleteByUserId(id);

    }

}
