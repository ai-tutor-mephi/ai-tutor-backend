package com.VLmb.ai_tutor_backend.feature.dialog.integration;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.auth.infra.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.shared.error.ErrorResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SendMessageIntegrationTest {

    private static final String AUTH_REGISTER = "/api/auth/register";
    private static final String AUTH_LOGIN = "/api/auth/login";
    private static final String SEND_QUESTION = "/api/dialogs/%d/send-question";
    private static final String RAG_QUERY = "/query";

    private String accessToken;
    private Long dialogId;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("clients.rag.base-url", wireMock::baseUrl);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        accessToken = registerAndLoginUser(
                "test-user",
                "test-user@email.com",
                "pwd8888888"
        );
        User user = userRepository.findByUserName("test-user").orElseThrow();
        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle("test-dialog");
        dialogId = dialogRepository.save(dialog).getId();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        messageRepository.deleteAll();
        dialogRepository.deleteAll();
        userRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    void shouldSendQuestionToDialog() {
        stubRagAnswerSuccess("Привет, я глупая ИИшка!");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        SendMessageRequest request = new SendMessageRequest("Расскажи о себе");
        ResponseEntity<SendMessageResponse> response = restTemplate.exchange(
                SEND_QUESTION.formatted(dialogId),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                SendMessageResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Привет, я глупая ИИшка!", response.getBody().answer());
        assertEquals(2, messageRepository.count());
    }

    @Test
    void shouldReturnErrorWhenRagFails() {
        stubRagAnswerFailure();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        SendMessageRequest request = new SendMessageRequest("Расскажи о себе");
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                SEND_QUESTION.formatted(dialogId),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                ErrorResponse.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().status());
        assertTrue(response.getBody().message().contains("RAG-сервис вернул серверную ошибку"));
        assertTrue(response.getBody().message().contains("RAG temporarily unavailable"));
        assertTrue(response.getBody().message().contains("Наша поддержка в tg: @WocherZ"));
        assertEquals(0, messageRepository.count());
    }

    private String registerAndLoginUser(String username, String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        restTemplate.exchange(
                AUTH_REGISTER,
                HttpMethod.POST,
                new HttpEntity<>(registerRequest, headers),
                String.class
        );

        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.exchange(
                AUTH_LOGIN,
                HttpMethod.POST,
                new HttpEntity<>(loginRequest, headers),
                LoginResponse.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().accessToken());
        return loginResponse.getBody().accessToken();
    }

    private void stubRagAnswerSuccess(String answer) {
        wireMock.stubFor(post(urlEqualTo(RAG_QUERY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"answer":"%s"}
                """.formatted(answer))));
    }

    private void stubRagAnswerFailure() {
        wireMock.stubFor(post(urlEqualTo(RAG_QUERY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {"error":"RAG temporarily unavailable"}
                """)));
    }

}
