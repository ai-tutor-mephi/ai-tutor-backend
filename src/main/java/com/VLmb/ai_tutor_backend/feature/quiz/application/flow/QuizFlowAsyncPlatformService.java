package com.VLmb.ai_tutor_backend.feature.quiz.application.flow;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.application.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Profile("platform")
@RequiredArgsConstructor
public class QuizFlowAsyncPlatformService implements QuizFlowService {

    private final QuizService quizService;

    @Override
    public CompletableFuture<QuizResponse> createQuiz(Long dialogId, User user) {
        return quizService.createQuizAsync(dialogId, user);
    }
}
