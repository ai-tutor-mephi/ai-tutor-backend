package com.VLmb.ai_tutor_backend.feature.quiz.application.flow;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;

import java.util.concurrent.CompletableFuture;

public interface QuizFlowService {

    CompletableFuture<QuizResponse> createQuiz(Long dialogId, Integer questionsCount, User user);
}
