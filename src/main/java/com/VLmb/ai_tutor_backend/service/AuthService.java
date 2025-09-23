package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.AuthResponse;
import com.VLmb.ai_tutor_backend.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.dto.RegisterUserRequest;
import com.VLmb.ai_tutor_backend.entity.Role;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public void register(RegisterUserRequest request) throws IllegalStateException {
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new IllegalStateException("Username is already taken");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email is already in use");
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
        String refresh = jwtService.generateRefreshToken(principal);
        return new AuthResponse(access, refresh);
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        CustomUserDetails principal = new CustomUserDetails(user);
        if (!jwtService.isTokenValid(refreshToken, principal)) {
            throw new IllegalStateException("Invalid refresh token");
        }
        String newAccess = jwtService.generateAccessToken(principal);
        return new AuthResponse(newAccess, refreshToken);
    }

}
