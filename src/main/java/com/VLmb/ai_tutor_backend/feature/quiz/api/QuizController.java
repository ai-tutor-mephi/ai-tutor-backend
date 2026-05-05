package com.VLmb.ai_tutor_backend.feature.quiz.api;

import com.VLmb.ai_tutor_backend.feature.auth.application.CustomUserDetails;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.application.QuizService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(
            @RequestParam Long dialogId,
            @RequestParam @Min(1) Integer questionsCount,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
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
