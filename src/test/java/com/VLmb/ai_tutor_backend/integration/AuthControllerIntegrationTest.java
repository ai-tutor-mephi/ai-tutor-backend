package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.feature.auth.api.AuthController;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenRequest;
import com.VLmb.ai_tutor_backend.feature.auth.infra.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.VLmb.ai_tutor_backend.integration.TestEndpoints.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearDb() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RegisterRequest registerRequest = new RegisterRequest(
                "testUserName",
                "test@email.com",
                "testPassword"
        );

        HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                AUTH_REGISTER,
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldLoginUserAfterRegistration() {

        registerUser("testUserName", "test@email.com", "testPassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LoginRequest loginRequest = new LoginRequest(
                "testUserName",
                "testPassword"
        );

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> response = restTemplate.exchange(
                AUTH_LOGIN,
                HttpMethod.POST,
                request,
                LoginResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
        assertNotNull(response.getBody().refreshToken());

    }

    @Test
    void shouldLRefreshAccessToken() {

        registerUser("testUserName", "test@email.com", "testPassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LoginRequest loginRequest = new LoginRequest(
                "testUserName",
                "testPassword"
        );

        HttpEntity<LoginRequest> requestForLogin = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.exchange(
                AUTH_LOGIN,
                HttpMethod.POST,
                requestForLogin,
                LoginResponse.class
        );

        assertNotNull(loginResponse.getBody());
        String accessToken = loginResponse.getBody().accessToken();
        String refreshToken = loginResponse.getBody().refreshToken();

        var refreshRequest = new RefreshTokenRequest(refreshToken);

        HttpEntity<RefreshTokenRequest> request = new HttpEntity<>(refreshRequest, headers);

        ResponseEntity<RefreshTokenResponse> response = restTemplate.exchange(
                AUTH_REFRESH,
                HttpMethod.POST,
                request,
                RefreshTokenResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
        assertNotNull(response.getBody().refreshToken());
    }

    void registerUser(String username, String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RegisterRequest registerRequest = new RegisterRequest(
                username,
                email,
                password
        );

        HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                AUTH_REGISTER,
                HttpMethod.POST,
                request,
                String.class
        );
    }
}
