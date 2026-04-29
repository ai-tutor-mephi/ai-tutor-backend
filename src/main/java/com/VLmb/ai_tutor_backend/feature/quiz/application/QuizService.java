package com.VLmb.ai_tutor_backend.feature.quiz.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizQuestionResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.domain.Quiz;
import com.VLmb.ai_tutor_backend.feature.quiz.domain.QuizQuestion;
import com.VLmb.ai_tutor_backend.feature.quiz.infra.QuizRepository;
import com.VLmb.ai_tutor_backend.feature.rag.application.RagCommunicationService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final DialogRepository dialogRepository;
    private final RagCommunicationService ragCommunicationService;
    @Qualifier("dbExecutor")
    private final TaskExecutor dbExecutor;

    @Transactional
    public QuizResponse createQuiz(Long dialogId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        log.info(
                "event=quiz_create_start dialog_id={} user_id={}",
                dialogId,
                currentUser.getId()
        );

        QuizResponse generatedQuiz = ragCommunicationService.generateQuiz(dialogId);
        Quiz savedQuiz = quizRepository.save(toEntity(generatedQuiz, dialog));

        log.info(
                "event=quiz_create_success dialog_id={} user_id={} question_count={}",
                dialogId,
                currentUser.getId(),
                savedQuiz.getQuestions().size()
        );

        return toDto(savedQuiz);
    }

    public CompletableFuture<QuizResponse> createQuizAsync(Long dialogId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        log.info(
                "event=quiz_create_async_start dialog_id={} user_id={}",
                dialogId,
                currentUser.getId()
        );

        return ragCommunicationService.generateQuizAsync(dialogId)
                .thenApplyAsync(generatedQuiz -> {
                    Quiz savedQuiz = quizRepository.save(toEntity(generatedQuiz, dialog));
                    log.info(
                            "event=quiz_create_async_success dialog_id={} user_id={} question_count={}",
                            dialogId,
                            currentUser.getId(),
                            savedQuiz.getQuestions().size()
                    );
                    return toDto(savedQuiz);
                }, dbExecutor);
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long dialogId, Long quizId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        Quiz quiz = quizRepository.findByIdAndDialogId(quizId, dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        return toDto(quiz);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByDialogId(Long dialogId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        return quizRepository.findByDialogIdOrderByCreatedAtDesc(dialogId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Quiz toEntity(QuizResponse quizResponse, Dialog dialog) {
        Quiz quiz = new Quiz();
        quiz.setTestName(quizResponse.testName());
        quiz.setDialog(dialog);

        List<QuizQuestion> questions = new ArrayList<>();
        List<QuizQuestionResponse> questionResponses = quizResponse.questions();
        if (questionResponses != null) {
            for (int i = 0; i < questionResponses.size(); i++) {
                QuizQuestionResponse questionResponse = questionResponses.get(i);
                QuizQuestion question = new QuizQuestion();
                question.setQuestion(questionResponse.question());
                question.setVariants(new ArrayList<>(questionResponse.variants()));
                question.setGoldAnswer(questionResponse.goldAnswer());
                question.setQuestionOrder(i + 1);
                question.setQuiz(quiz);
                questions.add(question);
            }
        }
        quiz.setQuestions(questions);
        return quiz;
    }

    private QuizResponse toDto(Quiz quiz) {
        return new QuizResponse(
                quiz.getTestName(),
                quiz.getQuestions().stream()
                        .map(question -> new QuizQuestionResponse(
                                question.getQuestion(),
                                question.getVariants(),
                                question.getGoldAnswer()
                        ))
                        .toList()
        );
    }

    private Dialog getDialog(Long dialogId) {
        return dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));
    }

    private void assertDialogOwner(Dialog dialog, User currentUser) {
        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("User does not have permission to access this dialog");
        }
    }
}
