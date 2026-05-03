package com.VLmb.ai_tutor_backend.feature.quiz.integration;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.auth.infra.RefreshTokenRepository;
import com.VLmb.ai_tutor_backend.feature.auth.infra.UserRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizAnswerRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.infra.QuizRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "virtual"})
class QuizIntegrationTest {

    private static final String AUTH_REGISTER = "/api/auth/register";
    private static final String AUTH_LOGIN = "/api/auth/login";
    private static final String CREATE_QUIZ = "/api/quiz?dialogId=%d";
    private static final String SCORE_QUIZ = "/api/quiz/%d/score";
    private static final String RAG_TEST = "/test";

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
    private QuizRepository quizRepository;

    private String accessToken;
    private Long dialogId;

    @BeforeEach
    void setUp() {
        accessToken = registerAndLoginUser("quiz-user", "quiz-user@email.com", "pwd8888888");

        User user = userRepository.findByUserName("quiz-user").orElseThrow();

        Dialog dialog = new Dialog();
        dialog.setOwner(user);
        dialog.setTitle("quiz-dialog");
        dialogId = dialogRepository.save(dialog).getId();

        Message firstMessage = new Message();
        firstMessage.setDialog(dialog);
        firstMessage.setRole(Message.MessageRole.USER);
        firstMessage.setContent("What is Java?");
        messageRepository.save(firstMessage);

        Message secondMessage = new Message();
        secondMessage.setDialog(dialog);
        secondMessage.setRole(Message.MessageRole.BOT);
        secondMessage.setContent("Java is a programming language.");
        messageRepository.save(secondMessage);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        messageRepository.deleteAll();
        quizRepository.deleteAll();
        dialogRepository.deleteAll();
        userRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    void shouldCreateQuizFromDialogHistory() {
        stubGenerateQuizSuccess();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        ResponseEntity<QuizResponse> response = restTemplate.exchange(
                CREATE_QUIZ.formatted(dialogId),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                QuizResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals("Java Basics", response.getBody().testName());
        assertEquals(2, response.getBody().questions().size());
        assertNotNull(response.getBody().questions().get(0).id());
        assertNotNull(response.getBody().questions().get(1).id());
        assertEquals(1, quizRepository.count());

        var savedQuiz = quizRepository.findByDialogIdOrderByCreatedAtDesc(dialogId).getFirst();
        assertEquals("Java Basics", savedQuiz.getTestName());
        assertEquals(2, savedQuiz.getQuestions().size());
        assertEquals(1, savedQuiz.getQuestions().get(0).getQuestionOrder());
        assertEquals(2, savedQuiz.getQuestions().get(1).getQuestionOrder());

        wireMock.verify(postRequestedFor(urlEqualTo(RAG_TEST))
                .withRequestBody(matchingJsonPath("$.dialogId", equalTo(dialogId.toString())))
                .withRequestBody(matchingJsonPath("$.dialogMessages[0].role", equalTo("USER")))
                .withRequestBody(matchingJsonPath("$.dialogMessages[0].message", equalTo("What is Java?")))
                .withRequestBody(matchingJsonPath("$.dialogMessages[1].role", equalTo("BOT")))
                .withRequestBody(matchingJsonPath("$.dialogMessages[1].message", equalTo("Java is a programming language."))));
    }

    @Test
    void shouldScoreQuizAnswers() {
        stubGenerateQuizSuccess();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        ResponseEntity<QuizResponse> createResponse = restTemplate.exchange(
                CREATE_QUIZ.formatted(dialogId),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                QuizResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());

        QuizScoreRequest scoreRequest = new QuizScoreRequest(List.of(
                new QuizAnswerRequest(createResponse.getBody().questions().get(0).id(), "Language"),
                new QuizAnswerRequest(createResponse.getBody().questions().get(1).id(), "Operating system")
        ));

        ResponseEntity<QuizScoreResponse> scoreResponse = restTemplate.exchange(
                SCORE_QUIZ.formatted(createResponse.getBody().id()),
                HttpMethod.POST,
                new HttpEntity<>(scoreRequest, headers),
                QuizScoreResponse.class
        );

        assertEquals(HttpStatus.OK, scoreResponse.getStatusCode());
        assertNotNull(scoreResponse.getBody());
        assertEquals(createResponse.getBody().id(), scoreResponse.getBody().quizId());
        assertEquals(2, scoreResponse.getBody().totalQuestions());
        assertEquals(1, scoreResponse.getBody().correctAnswers());
        assertEquals(0.5, scoreResponse.getBody().score());
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

    private void stubGenerateQuizSuccess() {
        wireMock.stubFor(post(urlEqualTo(RAG_TEST))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "test_name": "Java Basics",
                                  "questions": [
                                    {
                                      "question": "What is Java?",
                                      "variants": ["Language", "Database", "Browser"],
                                      "gold_answer": "Language"
                                    },
                                    {
                                      "question": "What was discussed?",
                                      "variants": ["Programming language", "Operating system"],
                                      "gold_answer": "Programming language"
                                    }
                                  ]
                                }
                                """)));
    }
}
