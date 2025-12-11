package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.dto.*;
import com.VLmb.ai_tutor_backend.repository.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

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
    void shouldChangeUsername() {
        String accessToken = registerAndLoginUser("oldName", "old@email.com", "password1234");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        ChangeUsernameRequest requestBody = new ChangeUsernameRequest("newName");
        HttpEntity<ChangeUsernameRequest> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<ChangeUsernameResponse> response = restTemplate.exchange(
                "/api/user/change-username",
                HttpMethod.POST,
                request,
                ChangeUsernameResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newName", response.getBody().userName());
        assertTrue(userRepository.findByUserName("newName").isPresent());
    }

    @Test
    void shouldFailWhenUsernameAlreadyExists() {
        registerAndLoginUser("existing", "existing@email.com", "password1234");
        String accessToken = registerAndLoginUser("another", "another@email.com", "password1234");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        ChangeUsernameRequest requestBody = new ChangeUsernameRequest("existing");
        HttpEntity<ChangeUsernameRequest> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/user/change-username",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    private String registerAndLoginUser(String username, String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RegisterUserRequest registerRequest = new RegisterUserRequest(
                username,
                email,
                password
        );

        HttpEntity<RegisterUserRequest> request = new HttpEntity<>(registerRequest, headers);

        restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                request,
                String.class
        );

        LoginRequest loginRequest = new LoginRequest(
                username,
                password
        );

        HttpEntity<LoginRequest> requestForLogin = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                requestForLogin,
                AuthResponse.class
        );

        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().accessToken());

        return loginResponse.getBody().accessToken();
    }
}
