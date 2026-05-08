package com.VLmb.ai_tutor_backend.feature.quiz.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizAnswerRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizQuestionResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizQuestionScoreResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreRequest;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizScoreResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.domain.Quiz;
import com.VLmb.ai_tutor_backend.feature.quiz.domain.QuizQuestion;
import com.VLmb.ai_tutor_backend.feature.quiz.infra.QuizRepository;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizQuestionResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizResponse;
import com.VLmb.ai_tutor_backend.feature.rag.application.RagCommunicationService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final DialogRepository dialogRepository;
    private final RagCommunicationService ragCommunicationService;

    @Transactional
    public QuizResponse createQuiz(Long dialogId, Integer questionsCount, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);

        log.info(
                "event=quiz_create_start dialog_id={} user_id={} questions_count={}",
                dialogId,
                currentUser.getId(),
                questionsCount
        );

        RagQuizResponse generatedQuiz = ragCommunicationService.generateQuiz(dialogId, questionsCount);
        Quiz savedQuiz = quizRepository.save(toEntity(generatedQuiz, dialog));

        log.info(
                "event=quiz_create_success dialog_id={} user_id={} question_count={}",
                dialogId,
                currentUser.getId(),
                savedQuiz.getQuestions().size()
        );

        return toDto(savedQuiz);
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

    @Transactional(readOnly = true)
    public QuizScoreResponse scoreQuiz(Long quizId, QuizScoreRequest request, User currentUser) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        assertDialogOwner(quiz.getDialog(), currentUser);

        Map<Long, String> submittedAnswers = new HashMap<>();
        for (QuizAnswerRequest answer : request.answers()) {
            submittedAnswers.put(answer.questionId(), answer.answer());
        }

        List<QuizQuestionScoreResponse> questionResults = quiz.getQuestions()
                .stream()
                .map(question -> {
                    String selectedAnswer = submittedAnswers.get(question.getId());
                    boolean correct = normalizeAnswer(selectedAnswer).equals(normalizeAnswer(question.getGoldAnswer()));
                    return new QuizQuestionScoreResponse(
                            question.getId(),
                            selectedAnswer,
                            question.getGoldAnswer(),
                            correct
                    );
                })
                .toList();

        int correctAnswers = (int) questionResults.stream()
                .filter(QuizQuestionScoreResponse::correct)
                .count();
        int totalQuestions = questionResults.size();
        double score = totalQuestions == 0 ? 0.0 : (double) correctAnswers / totalQuestions;

        return new QuizScoreResponse(
                quiz.getId(),
                totalQuestions,
                correctAnswers,
                score,
                questionResults
        );
    }

    private Quiz toEntity(RagQuizResponse quizResponse, Dialog dialog) {
        Quiz quiz = new Quiz();
        quiz.setTestName(quizResponse.testName());
        quiz.setDialog(dialog);

        List<QuizQuestion> questions = new ArrayList<>();
        List<RagQuizQuestionResponse> questionResponses = quizResponse.questions();
        if (questionResponses != null) {
            for (int i = 0; i < questionResponses.size(); i++) {
                RagQuizQuestionResponse questionResponse = questionResponses.get(i);
                QuizQuestion question = new QuizQuestion();
                question.setQuestion(questionResponse.question());
                question.setVariants(shuffledVariants(questionResponse.variants()));
                question.setGoldAnswer(questionResponse.goldAnswer());
                question.setQuestionOrder(i + 1);
                question.setQuiz(quiz);
                questions.add(question);
            }
        }
        quiz.setQuestions(questions);
        return quiz;
    }

    private List<String> shuffledVariants(List<String> variants) {
        List<String> shuffled = new ArrayList<>(variants);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    private QuizResponse toDto(Quiz quiz) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getTestName(),
                quiz.getQuestions().stream()
                        .map(question -> new QuizQuestionResponse(
                                question.getId(),
                                question.getQuestion(),
                                question.getVariants()
                        ))
                        .toList()
        );
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim();
    }

    private Dialog getDialog(Long dialogId) {
        return dialogRepository.findById(dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Dialog", "id", dialogId));
    }

    private void assertDialogOwner(Dialog dialog, User currentUser) {
        if (!dialog.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("У пользователя нет доступа к этому диалогу.");
        }
    }
}
