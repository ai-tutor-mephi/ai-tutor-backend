package com.VLmb.ai_tutor_backend.feature.quiz.api;

import com.VLmb.ai_tutor_backend.feature.auth.application.CustomUserDetails;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.application.QuizService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.InvalidQuizQuestionsCountException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private static final int MIN_QUESTIONS_COUNT = 1;
    private static final int MAX_QUESTIONS_COUNT = 20;

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(
            @RequestParam Long dialogId,
            @Parameter(
                    description = "Количество вопросов в квизе.",
                    schema = @Schema(minimum = "1", maximum = "20")
            )
            @RequestParam Integer questionsCount,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (questionsCount < MIN_QUESTIONS_COUNT || questionsCount > MAX_QUESTIONS_COUNT) {
            throw new InvalidQuizQuestionsCountException(MIN_QUESTIONS_COUNT, MAX_QUESTIONS_COUNT);
        }

        QuizResponse response = quizService.createQuiz(dialogId, questionsCount, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<QuizResponse>> getQuizzesByDialogId(
            @RequestParam Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<QuizResponse> response = quizService.getQuizzesByDialogId(dialogId, principal.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuiz(
            @PathVariable Long quizId,
            @RequestParam Long dialogId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        QuizResponse response = quizService.getQuiz(dialogId, quizId, principal.getUser());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{quizId}/score")
    public ResponseEntity<QuizScoreResponse> scoreQuiz(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizScoreRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        QuizScoreResponse response = quizService.scoreQuiz(quizId, request, principal.getUser());
        return ResponseEntity.ok(response);
    }
}
