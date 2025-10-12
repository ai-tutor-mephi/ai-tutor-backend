package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.dto.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "clients.rag.base-url=http://localhost:${wiremock.server.port}"
})
@ActiveProfiles("test")
public class SendMessageToDialogIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void resetWireMock() {
        com.github.tomakehurst.wiremock.client.WireMock.reset();
    }

    @Test
    void shouldCallExternalServiceViaClient() {

        stubFor(post(urlEqualTo("/rag/user-question"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
            {
              "answer": "Yeah, you're absolutely right, my friend."
            }
        """)));

        String accessToken = registerAndLoginUser("username", "email@email.com", "password1234");

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-file-1.txt"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> response = restTemplate.exchange(
                "/api/dialogs/with-files",
                HttpMethod.POST,
                requestEntity,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().dialogId());
        assertNotNull(response.getBody().title());

        headers.setContentType(MediaType.APPLICATION_JSON);
        MessageRequest messageRequest = new MessageRequest("Is Java the best language for backend development?");
        HttpEntity<MessageRequest> requestEntityForRag = new HttpEntity<>(messageRequest, headers);

        ResponseEntity<MessageResponse> answer = restTemplate.exchange(
                "/api/dialogs/%d/send-question".formatted(response.getBody().dialogId()),
                HttpMethod.POST,
                requestEntityForRag,
                MessageResponse.class
        );

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertNotNull(answer.getBody());
        assertEquals("Yeah, you're absolutely right, my friend.", answer.getBody().answer());

        // 4️⃣ Проверяем, что WireMock реально получил запрос
        verify(postRequestedFor(urlEqualTo("/rag/user-question")));
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

        ResponseEntity<String> response = restTemplate.exchange(
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
