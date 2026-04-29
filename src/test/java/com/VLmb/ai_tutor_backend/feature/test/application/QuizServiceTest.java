package com.VLmb.ai_tutor_backend.feature.test.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.test.domain.Quiz;
import com.VLmb.ai_tutor_backend.feature.test.domain.QuizQuestion;
import com.VLmb.ai_tutor_backend.feature.test.infra.QuizRepository;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private QuizService quizService;

    private User owner;
    private Dialog dialog;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);

        dialog = new Dialog();
        dialog.setId(10L);
        dialog.setOwner(owner);
    }

    @Test
    void shouldSaveQuizForDialog() {
        Quiz quiz = new Quiz();
        quiz.setTestName("Quiz 1");

        QuizQuestion question = new QuizQuestion();
        question.setQuestion("Q1");
        question.setVariants(List.of("A", "B"));
        question.setGoldAnswer("A");
        question.setQuestionOrder(1);
        quiz.setQuestions(new java.util.ArrayList<>(List.of(question)));

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quiz saved = quizService.saveQuiz(10L, owner, quiz);

        assertSame(dialog, saved.getDialog());
        assertEquals(1, saved.getQuestions().size());
        assertSame(saved, saved.getQuestions().get(0).getQuiz());
        verify(quizRepository).save(quiz);
    }

    @Test
    void shouldReturnQuizByDialogId() {
        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.setDialog(dialog);
        quiz.setQuestions(List.of());

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(quizRepository.findByIdAndDialogId(100L, 10L)).thenReturn(Optional.of(quiz));

        Quiz actual = quizService.getQuiz(10L, 100L, owner);

        assertSame(quiz, actual);
    }

    @Test
    void shouldReturnAllQuizzesForDialog() {
        Quiz first = new Quiz();
        Quiz second = new Quiz();

        when(dialogRepository.findById(10L)).thenReturn(Optional.of(dialog));
        when(quizRepository.findByDialogIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(first, second));

        List<Quiz> actual = quizService.getQuizzesByDialogId(10L, owner);

        assertEquals(2, actual.size());
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
