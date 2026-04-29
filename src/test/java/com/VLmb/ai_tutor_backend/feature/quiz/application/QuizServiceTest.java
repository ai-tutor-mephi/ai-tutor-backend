package com.VLmb.ai_tutor_backend.feature.quiz.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizQuestionResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.domain.Quiz;
import com.VLmb.ai_tutor_backend.feature.quiz.infra.QuizRepository;
import com.VLmb.ai_tutor_backend.feature.rag.application.RagCommunicationService;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.task.SyncTaskExecutor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
                ragCommunicationService,
                new SyncTaskExecutor()
        );

        owner = new User();
        owner.setId(1L);

        dialog = new Dialog();
        dialog.setId(10L);
        dialog.setOwner(owner);
    }

    @Test
    void shouldCreateQuizAsyncForDialog() {
        QuizResponse generatedQuiz = new QuizResponse(
                "Quiz 1",
                List.of(new QuizQuestionResponse("Q1", List.of("A", "B"), "A"))
        );

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(ragCommunicationService.generateQuizAsync(10L)).thenReturn(CompletableFuture.completedFuture(generatedQuiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResponse saved = quizService.createQuizAsync(10L, owner).join();

        assertEquals("Quiz 1", saved.testName());
        assertEquals(1, saved.questions().size());
        verify(ragCommunicationService).generateQuizAsync(10L);
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void shouldCreateQuizForDialog() {
        QuizResponse generatedQuiz = new QuizResponse(
                "Quiz 1",
                List.of(new QuizQuestionResponse("Q1", List.of("A", "B"), "A"))
        );

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(ragCommunicationService.generateQuiz(10L)).thenReturn(generatedQuiz);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResponse saved = quizService.createQuiz(10L, owner);

        assertEquals("Quiz 1", saved.testName());
        assertEquals(1, saved.questions().size());
        assertEquals("A", saved.questions().get(0).goldAnswer());
        verify(ragCommunicationService).generateQuiz(10L);
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
    void shouldThrowWhenDialogNotFound() {
        when(dialogRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> quizService.getQuizzesByDialogId(10L, owner)
        );

        assertNotNull(ex.getMessage());
    }
}
