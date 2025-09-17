package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.dto.AuthResponse;
import com.VLmb.ai_tutor_backend.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.dto.RegisterUserRequest;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import com.VLmb.ai_tutor_backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
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
        newUser.setHashPassword(passwordEncoder.encode(request.password()));

        userRepository.save(newUser);

    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return new AuthResponse(jwt);
    }

}
