package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.VLmb.ai_tutor_backend.integration.TestEndpoints.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "clients.rag.base-url=http://localhost:${wiremock.server.port}"
})
@ActiveProfiles("test")
public class SendMessageToDialogIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockitoSpyBean
    private com.VLmb.ai_tutor_backend.feature.rag.infra.RagRestClient ragRestClient;

    @AfterEach
    void resetWireMock() {
        com.github.tomakehurst.wiremock.client.WireMock.reset();
        Mockito.reset(ragRestClient);
    }

    @Test
    void shouldCallExternalServiceViaClient() {
        stubFor(post(urlEqualTo(RAG_QUERY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
            {
              "answer": "Yeah, you're absolutely right, my friend."
            }
        """)));

        stubFor(post(urlEqualTo(RAG_LOAD))
                .willReturn(aResponse()
                        .withStatus(200)));

        String accessToken = registerAndLoginUser("username", "email@email.com", "password1234");

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-file-1.txt"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<CreateDialogResponse> response = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                requestEntity,
                CreateDialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().dialogId());
        assertNotNull(response.getBody().title());

        headers.setContentType(MediaType.APPLICATION_JSON);
        SendMessageRequest messageRequest = new SendMessageRequest("Is Java the best language for backend development?");
        HttpEntity<SendMessageRequest> requestEntityForRag = new HttpEntity<>(messageRequest, headers);

        ResponseEntity<SendMessageResponse> answer = restTemplate.exchange(
                DIALOG_SEND_QUESTION.formatted(response.getBody().dialogId()),
                HttpMethod.POST,
                requestEntityForRag,
                SendMessageResponse.class
        );

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertNotNull(answer.getBody());
        assertEquals("Yeah, you're absolutely right, my friend.", answer.getBody().answer());

        verify(postRequestedFor(urlEqualTo(RAG_QUERY)));
    }

    @Test
    void shouldSendMessageToDialogWithMockedRagService() {
        String accessToken = registerAndLoginUser("dialogUser", "dialogUser@email.com", "password1234");

        stubFor(post(urlEqualTo(RAG_LOAD))
                .willReturn(aResponse()
                        .withStatus(200)));

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-file-1.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<CreateDialogResponse> dialogResponse = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                requestEntity,
                CreateDialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, dialogResponse.getStatusCode());
        assertNotNull(dialogResponse.getBody());
        Long dialogId = dialogResponse.getBody().dialogId();
        assertNotNull(dialogId);

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

        mockRagServiceAnswer("Second answer from RAG");

        headers.setContentType(MediaType.APPLICATION_JSON);
        SendMessageRequest messageRequest = new SendMessageRequest("Second question?");
        HttpEntity<SendMessageRequest> requestEntityForSend = new HttpEntity<>(messageRequest, headers);

        ResponseEntity<SendMessageResponse> response = restTemplate.exchange(
                DIALOG_SEND_QUESTION.formatted(dialogId),
                HttpMethod.POST,
                requestEntityForSend,
                SendMessageResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Second answer from RAG", response.getBody().answer());

        List<Message> messages = messageRepository.findByDialogId(dialogId);
        assertEquals(4, messages.size());
        assertTrue(messages.stream().anyMatch(m -> "Second answer from RAG".equals(m.getContent())));
    }

    private String registerAndLoginUser(String username, String email, String password) {
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

        LoginRequest loginRequest = new LoginRequest(
                username,
                password
        );

        HttpEntity<LoginRequest> requestForLogin = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.exchange(
                AUTH_LOGIN,
                HttpMethod.POST,
                requestForLogin,
                LoginResponse.class
        );

        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().accessToken());

        return loginResponse.getBody().accessToken();
    }

    private void mockRagServiceAnswer(String answer) {
        Mockito.doReturn(new SendMessageResponse(answer))
                .when(ragRestClient)
                .sendMessage(Mockito.any());
    }

}
