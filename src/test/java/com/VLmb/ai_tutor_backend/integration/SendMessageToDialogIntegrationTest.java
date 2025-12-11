package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.dto.*;
import com.VLmb.ai_tutor_backend.entity.Dialog;
import com.VLmb.ai_tutor_backend.entity.Message;
import com.VLmb.ai_tutor_backend.repository.DialogRepository;
import com.VLmb.ai_tutor_backend.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "clients.rag.base-url=http://localhost:${wiremock.server.port}"
})
@ActiveProfiles("test")
public class SendMessageToDialogIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SendMessageToDialogIntegrationTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @SpyBean
    private com.VLmb.ai_tutor_backend.client.RagRestClient ragRestClient;

    @AfterEach
    void resetWireMock() {
        com.github.tomakehurst.wiremock.client.WireMock.reset();
        Mockito.reset(ragRestClient);
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
        log.info("Authorized user for shouldCallExternalServiceViaClient test");

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
        log.info("Created dialog {} with initial file", response.getBody().dialogId());

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
        log.info("Received answer from RAG: {}", answer.getBody().answer());

        // 4️⃣ Проверяем, что WireMock реально получил запрос
        verify(postRequestedFor(urlEqualTo("/rag/user-question")));
    }

    @Test
    void shouldSendMessageToDialogWithMockedRagService() {
        String accessToken = registerAndLoginUser("dialogUser", "dialogUser@email.com", "password1234");
        log.info("Authorized user for shouldSendMessageToDialogWithMockedRagService test");

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-file-1.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> dialogResponse = restTemplate.exchange(
                "/api/dialogs/with-files",
                HttpMethod.POST,
                requestEntity,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, dialogResponse.getStatusCode());
        assertNotNull(dialogResponse.getBody());
        Long dialogId = dialogResponse.getBody().dialogId();
        assertNotNull(dialogId);
        log.info("Created dialog {} with seeded file", dialogId);

        Dialog dialog = dialogRepository.findById(dialogId).orElseThrow();

        Message firstMessage = new Message();
        firstMessage.setDialog(dialog);
        firstMessage.setRole(Message.MessageRole.USER);
        firstMessage.setContent("First question");

        Message secondMessage = new Message();
        secondMessage.setDialog(dialog);
        secondMessage.setRole(Message.MessageRole.BOT);
        secondMessage.setContent("First answer");

        messageRepository.save(firstMessage);
        messageRepository.save(secondMessage);
        log.info("Seeded dialog {} with initial messages", dialogId);

        mockRagServiceAnswer("Second answer from RAG");

        headers.setContentType(MediaType.APPLICATION_JSON);
        MessageRequest messageRequest = new MessageRequest("Second question?");
        HttpEntity<MessageRequest> requestEntityForSend = new HttpEntity<>(messageRequest, headers);

        ResponseEntity<MessageResponse> response = restTemplate.exchange(
                "/api/dialogs/%d/send-question".formatted(dialogId),
                HttpMethod.POST,
                requestEntityForSend,
                MessageResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Second answer from RAG", response.getBody().answer());
        log.info("Sent question to dialog {} and received mocked answer {}", dialogId, response.getBody().answer());

        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(4, messages.size());
        assertTrue(messages.stream().anyMatch(m -> "Second answer from RAG".equals(m.getContent())));
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

    private void mockRagServiceAnswer(String answer) {
        Mockito.doReturn(new MessageResponse(answer))
                .when(ragRestClient)
                .current(Mockito.any());
    }

}
