package com.VLmb.ai_tutor_backend.feature.quiz.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizAnswerRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private DialogRepository dialogRepository;

    @Mock
    private RagCommunicationService ragCommunicationService;

    private QuizService quizService;

    private User owner;
    private Dialog dialog;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(
                quizRepository,
                dialogRepository,
                ragCommunicationService
        );

        owner = new User();
        owner.setId(1L);

        dialog = new Dialog();
        dialog.setId(10L);
        dialog.setOwner(owner);
    }

    @Test
    void shouldCreateQuizForDialog() {
        RagQuizResponse generatedQuiz = new RagQuizResponse(
                "Quiz 1",
                List.of(new RagQuizQuestionResponse("Q1", List.of("A", "B"), "A"))
        );

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(ragCommunicationService.generateQuiz(10L, 3)).thenReturn(generatedQuiz);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResponse saved = quizService.createQuiz(10L, 3, owner);

        assertNull(saved.id());
        assertEquals("Quiz 1", saved.testName());
        assertEquals(1, saved.questions().size());
        assertEquals("Q1", saved.questions().get(0).question());
        verify(ragCommunicationService).generateQuiz(10L, 3);
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void shouldReturnQuizByDialogId() {
        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.setTestName("Quiz 1");
        quiz.setDialog(dialog);
        quiz.setQuestions(List.of());

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(quizRepository.findByIdAndDialogId(100L, 10L)).thenReturn(Optional.of(quiz));

        QuizResponse actual = quizService.getQuiz(10L, 100L, owner);

        assertEquals(100L, actual.id());
        assertEquals("Quiz 1", actual.testName());
    }

    @Test
    void shouldReturnAllQuizzesForDialog() {
        Quiz first = new Quiz();
        first.setTestName("Quiz 1");
        Quiz second = new Quiz();
        second.setTestName("Quiz 2");

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(quizRepository.findByDialogIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(first, second));

        List<QuizResponse> actual = quizService.getQuizzesByDialogId(10L, owner);

        assertEquals(2, actual.size());
        assertEquals("Quiz 1", actual.get(0).testName());
    }

    @Test
    void shouldScoreQuizByQuestionAnswers() {
        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.setDialog(dialog);
        quiz.setTestName("Quiz 1");

        QuizQuestion firstQuestion = new QuizQuestion();
        firstQuestion.setId(1L);
        firstQuestion.setQuiz(quiz);
        firstQuestion.setQuestion("Q1");
        firstQuestion.setVariants(List.of("A", "B"));
        firstQuestion.setGoldAnswer("A");
        firstQuestion.setQuestionOrder(1);

        QuizQuestion secondQuestion = new QuizQuestion();
        secondQuestion.setId(2L);
        secondQuestion.setQuiz(quiz);
        secondQuestion.setQuestion("Q2");
        secondQuestion.setVariants(List.of("C", "D"));
        secondQuestion.setGoldAnswer("D");
        secondQuestion.setQuestionOrder(2);

        quiz.setQuestions(List.of(firstQuestion, secondQuestion));

        when(quizRepository.findByIdWithQuestions(100L)).thenReturn(Optional.of(quiz));

        QuizScoreResponse actual = quizService.scoreQuiz(
                100L,
                new QuizScoreRequest(List.of(
                        new QuizAnswerRequest(1L, "A"),
                        new QuizAnswerRequest(2L, "C")
                )),
                owner
        );

        assertEquals(100L, actual.quizId());
        assertEquals(2, actual.totalQuestions());
        assertEquals(1, actual.correctAnswers());
        assertEquals(0.5, actual.score());
        assertEquals(true, actual.questions().get(0).correct());
        assertEquals(false, actual.questions().get(1).correct());
    }

    @Test
    void shouldThrowWhenDialogNotFound() {
        when(dialogRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> quizService.getQuizzesByDialogId(10L, owner)
        );

        assertNotNull(ex.getMessage());
    }
}
