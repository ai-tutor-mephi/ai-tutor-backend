package com.VLmb.ai_tutor_backend.feature.dialog.integration;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.infra.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import com.VLmb.ai_tutor_backend.feature.file.application.FileStorageService;
import com.VLmb.ai_tutor_backend.feature.file.infra.FileMetadataRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DialogFlowIntegrationTest {

    private static final String AUTH_REGISTER = "/api/auth/register";
    private static final String AUTH_LOGIN = "/api/auth/login";
    private static final String DIALOGS_WITH_FILES = "/api/dialogs/with-files";
    private static final String DIALOG_FILES = "/api/dialogs/%d/files";
    private static final String RAG_LOAD = "/load";

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
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        messageRepository.deleteAll();
        fileMetadataRepository.deleteAll();
        dialogRepository.deleteAll();
        userRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    void shouldCreateDialogWithFiles() {
        stubRagLoadSuccess();
        doNothing().when(fileStorageService).uploadFile(anyString(), any(), anyLong());

        String accessToken = registerAndLoginUser(
                "user1-" + UUID.randomUUID().toString().substring(0, 8),
                "user1+" + UUID.randomUUID().toString().substring(0, 8) + "@email.com",
                "password1234"
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("request", new HttpEntity<>(new CreateDialogRequest("Custom dialog title"), jsonHeaders));
        body.add("files", new ClassPathResource("test-horse.txt"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        ResponseEntity<CreateDialogResponse> response = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                CreateDialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().dialogId());
        assertEquals("Custom dialog title", response.getBody().title());
        assertEquals(1, dialogRepository.count());
        assertEquals("Custom dialog title", dialogRepository.findById(response.getBody().dialogId()).orElseThrow().getTitle());
        assertEquals(1, fileMetadataRepository.count());
        verify(fileStorageService).uploadFile(anyString(), any(), anyLong());
    }

    @Test
    void shouldAddFilesToExistingDialog() {
        stubRagLoadSuccess();
        doNothing().when(fileStorageService).uploadFile(anyString(), any(), anyLong());

        String accessToken = registerAndLoginUser(
                "user2-" + UUID.randomUUID().toString().substring(0, 8),
                "user2+" + UUID.randomUUID().toString().substring(0, 8) + "@email.com",
                "password1234"
        );

        MultiValueMap<String, Object> createBody = new LinkedMultiValueMap<>();
        createBody.add("files", new ClassPathResource("test-horse.txt"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        ResponseEntity<CreateDialogResponse> createResponse = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                CreateDialogResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long dialogId = createResponse.getBody().dialogId();

        MultiValueMap<String, Object> updateBody = new LinkedMultiValueMap<>();
        updateBody.add("files", new ClassPathResource("test-donkey.docx"));
        updateBody.add("files", new ClassPathResource("test-penguin.pdf"));

        ResponseEntity<List<DialogFileResponse>> updateResponse = restTemplate.exchange(
                DIALOG_FILES.formatted(dialogId),
                HttpMethod.POST,
                new HttpEntity<>(updateBody, headers),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.CREATED, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody());
        assertEquals(2, updateResponse.getBody().size());
        assertEquals(3, fileMetadataRepository.count());
    }

    @Test
    void shouldReturn422WhenTextCannotBeExtracted() {
        doNothing().when(fileStorageService).uploadFile(anyString(), any(), anyLong());

        String accessToken = registerAndLoginUser(
                "user3-" + UUID.randomUUID().toString().substring(0, 8),
                "user3+" + UUID.randomUUID().toString().substring(0, 8) + "@email.com",
                "password1234"
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return "empty.txt";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        ResponseEntity<String> response = restTemplate.exchange(
                DIALOGS_WITH_FILES,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(0, dialogRepository.count());
        assertEquals(0, fileMetadataRepository.count());
        assertFalse(response.getBody() == null || response.getBody().isBlank());
        verify(fileStorageService, never()).uploadFile(anyString(), any(), anyLong());
    }

    private String registerAndLoginUser(String username, String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        ResponseEntity<String> registerResponse = restTemplate.exchange(
                AUTH_REGISTER,
                HttpMethod.POST,
                new HttpEntity<>(registerRequest, headers),
                String.class
        );
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

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

    private void stubRagLoadSuccess() {
        wireMock.stubFor(post(urlEqualTo(RAG_LOAD))
                .willReturn(aResponse().withStatus(200)));
    }
}
