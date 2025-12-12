package com.VLmb.ai_tutor_backend.integration;

import com.VLmb.ai_tutor_backend.dto.*;
import com.VLmb.ai_tutor_backend.repository.*;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.ParameterizedTypeReference;
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
import static org.mockito.Mockito.reset;
import static com.VLmb.ai_tutor_backend.integration.TestEndpoints.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "clients.rag.base-url=http://localhost:${wiremock.server.port}"
})
public class DialogControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @AfterEach
    void clearDatabase() {
        refreshTokenRepository.deleteAll();
        messageRepository.deleteAll();
        fileMetadataRepository.deleteAll();
        dialogRepository.deleteAll();
        userRepository.deleteAll();

        reset();
    }

    private void stubRag() {
        stubFor(post(urlPathMatching(RAG_LOAD))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "answer": "stub-answer-from-wiremock"
                                }
                                """)));
    }

    private void logRagRequests() {
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching(RAG_LOAD)));
        for (LoggedRequest req : requests) {
            System.out.println("=== RAG REQUEST ===");
            System.out.println("URL:   " + req.getUrl());
            System.out.println("BODY:  " + req.getBodyAsString());
            System.out.println("===================");
        }
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

        ResponseEntity<AuthResponse> loginResponse = restTemplate.exchange(
                AUTH_LOGIN,
                HttpMethod.POST,
                requestForLogin,
                AuthResponse.class
        );

        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().accessToken());

        return loginResponse.getBody().accessToken();
    }

    @Test
    void shouldCreateDialogWithFile() {
        stubRag();

        String accessToken = registerAndLoginUser("username", "email@email.com", "password1234");

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-horse.txt"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> response = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                requestEntity,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().dialogId());
        assertNotNull(response.getBody().title());

        logRagRequests();
    }

    @Test
    void shouldUpdateDialogWithFiles() {
        stubRag();

        String accessToken = registerAndLoginUser("username", "email@email.com", "password1234");

        FileSystemResource resource = new FileSystemResource(new File("src/test/resources/test-penguin.pdf"));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> response = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                requestEntity,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().dialogId());
        assertNotNull(response.getBody().title());

        HttpEntity<MultiValueMap<String, Object>> requestEntityForUpdate = getMultiValueMapHttpEntity(headers);

        var dialogId = response.getBody().dialogId();

        ResponseEntity<List<FileResponse>> responseForUpdate = restTemplate.exchange(
                DIALOG_FILES.formatted(dialogId),
                HttpMethod.POST,
                requestEntityForUpdate,
                new ParameterizedTypeReference<List<FileResponse>>() {}
        );

        assertEquals(HttpStatus.CREATED, responseForUpdate.getStatusCode());
        assertNotNull(responseForUpdate.getBody());
        assertNotNull(responseForUpdate.getBody().get(0));
        assertNotNull(responseForUpdate.getBody().get(1));
        assertNotNull(responseForUpdate.getBody().get(0).fileId());
        assertNotNull(responseForUpdate.getBody().get(1).fileId());

        logRagRequests();
    }

    private static HttpEntity<MultiValueMap<String, Object>> getMultiValueMapHttpEntity(HttpHeaders headers) {
        FileSystemResource resource1 = new FileSystemResource(new File("src/test/resources/test-horse.txt"));
        FileSystemResource resource2 = new FileSystemResource(new File("src/test/resources/test-donkey.docx"));

        MultiValueMap<String, Object> bodyForUpdate = new LinkedMultiValueMap<>();
        bodyForUpdate.add("files", resource1);
        bodyForUpdate.add("files", resource2);


        return new HttpEntity<>(bodyForUpdate, headers);
    }

    @Test
    void shouldGetFilesFromDialog() {
        stubRag();

        String accessToken = registerAndLoginUser("user1", "user1@email.com", "password1234");

        FileSystemResource file1 = new FileSystemResource(new File("src/test/resources/test-donkey.docx"));
        FileSystemResource file2 = new FileSystemResource(new File("src/test/resources/test-penguin.pdf"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", file1);
        body.add("files", file2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> createReq = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> createResp = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                createReq,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        Long dialogId = createResp.getBody().dialogId();
        assertNotNull(dialogId);

        HttpEntity<Void> getReq = new HttpEntity<>(null, new HttpHeaders() {{
            setBearerAuth(accessToken);
        }});

        ResponseEntity<List<FileResponse>> getResp = restTemplate.exchange(
                DIALOG_FILES.formatted(dialogId),
                HttpMethod.GET,
                getReq,
                new ParameterizedTypeReference<List<FileResponse>>() {}
        );

        assertEquals(HttpStatus.CREATED, getResp.getStatusCode());
        assertNotNull(getResp.getBody());
        assertEquals(2, getResp.getBody().size());
        assertNotNull(getResp.getBody().get(0).fileId());
        assertNotNull(getResp.getBody().get(1).fileId());
    }

    @Test
    void shouldGetAllDialogsForUser() {
        stubRag();

        String accessToken = registerAndLoginUser("user2", "user2@email.com", "password1234");

        FileSystemResource file = new FileSystemResource(new File("src/test/resources/test-horse.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> createReq = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> createResp = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                createReq,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        assertNotNull(createResp.getBody().dialogId());

        // GET /api/dialogs
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setBearerAuth(accessToken);

        HttpEntity<Void> getReq = new HttpEntity<>(null, getHeaders);

        ResponseEntity<List<DialogInfo>> listResp = restTemplate.exchange(
                DIALOGS,
                HttpMethod.GET,
                getReq,
                new ParameterizedTypeReference<List<DialogInfo>>() {}
        );

        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
        assertEquals(1, listResp.getBody().size());
        assertNotNull(listResp.getBody().get(0).dialogId());
        assertNotNull(listResp.getBody().get(0).title());
    }

    @Test
    void shouldChangeDialogTitle() {
        stubRag();

        String accessToken = registerAndLoginUser("user4", "user4@email.com", "password1234");

        FileSystemResource file = new FileSystemResource(new File("src/test/resources/test-horse.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> createReq = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> createResp = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                createReq,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        Long dialogId = createResp.getBody().dialogId();

        HttpHeaders patchHeaders = new HttpHeaders();
        patchHeaders.setBearerAuth(accessToken);
        patchHeaders.setContentType(MediaType.APPLICATION_JSON);

        ChangeDialogTitleRequest changeRequest = new ChangeDialogTitleRequest("Updated title");
        HttpEntity<ChangeDialogTitleRequest> patchReq = new HttpEntity<>(changeRequest, patchHeaders);

        ResponseEntity<DialogInfo> patchResp = restTemplate.exchange(
                DIALOG_CHANGE_TITLE.formatted(dialogId),
                HttpMethod.PATCH,
                patchReq,
                DialogInfo.class
        );

        assertEquals(HttpStatus.OK, patchResp.getStatusCode());
        assertNotNull(patchResp.getBody());
        assertEquals(dialogId, patchResp.getBody().dialogId());
        assertEquals("Updated title", patchResp.getBody().title());
    }

    @Test
    void shouldNotChangeDialogTitleWhenBlank() {
        stubRag();

        String accessToken = registerAndLoginUser("user5", "user5@email.com", "password1234");

        FileSystemResource file = new FileSystemResource(new File("src/test/resources/test-horse.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> createReq = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> createResp = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                createReq,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        Long dialogId = createResp.getBody().dialogId();

        HttpHeaders patchHeaders = new HttpHeaders();
        patchHeaders.setBearerAuth(accessToken);
        patchHeaders.setContentType(MediaType.APPLICATION_JSON);

        ChangeDialogTitleRequest changeRequest = new ChangeDialogTitleRequest(" ");
        HttpEntity<ChangeDialogTitleRequest> patchReq = new HttpEntity<>(changeRequest, patchHeaders);

        ResponseEntity<String> patchResp = restTemplate.exchange(
                DIALOG_CHANGE_TITLE.formatted(dialogId),
                HttpMethod.PATCH,
                patchReq,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, patchResp.getStatusCode());
    }

    @Test
    void shouldDeleteDialog() {
        stubRag();

        String accessToken = registerAndLoginUser("user3", "user3@email.com", "password1234");

        FileSystemResource file = new FileSystemResource(new File("src/test/resources/test-horse.txt"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, Object>> createReq = new HttpEntity<>(body, headers);

        ResponseEntity<DialogResponse> createResp = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                createReq,
                DialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        Long dialogId = createResp.getBody().dialogId();
        assertNotNull(dialogId);

        HttpHeaders delHeaders = new HttpHeaders();
        delHeaders.setBearerAuth(accessToken);

        HttpEntity<Void> delReq = new HttpEntity<>(null, delHeaders);

        ResponseEntity<Void> delResp = restTemplate.exchange(
                DIALOG_DELETE.formatted(dialogId),
                HttpMethod.DELETE,
                delReq,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, delResp.getStatusCode());

        HttpEntity<Void> listReq = new HttpEntity<>(null, delHeaders);

        ResponseEntity<List<DialogInfo>> listResp = restTemplate.exchange(
                DIALOGS,
                HttpMethod.GET,
                listReq,
                new ParameterizedTypeReference<List<DialogInfo>>() {}
        );

        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
        assertEquals(0, listResp.getBody().size());
    }
}
