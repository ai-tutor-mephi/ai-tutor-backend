package com.VLmb.ai_tutor_backend.feature.test.application;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.DialogRepository;
import com.VLmb.ai_tutor_backend.feature.test.domain.Quiz;
import com.VLmb.ai_tutor_backend.feature.test.domain.QuizQuestion;
import com.VLmb.ai_tutor_backend.feature.test.infra.QuizRepository;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final DialogRepository dialogRepository;

    @Transactional
    public Quiz saveQuiz(Long dialogId, User currentUser, Quiz quiz) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        bindRelations(quiz, dialog);
        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public Quiz getQuiz(Long dialogId, Long quizId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        return quizRepository.findByIdAndDialogId(quizId, dialogId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
    }

    @Transactional(readOnly = true)
    public List<Quiz> getQuizzesByDialogId(Long dialogId, User currentUser) {
        Dialog dialog = getDialog(dialogId);
        assertDialogOwner(dialog, currentUser);
        return quizRepository.findByDialogIdOrderByCreatedAtDesc(dialogId);
    }

    private void bindRelations(Quiz quiz, Dialog dialog) {
        quiz.setId(null);
        quiz.setDialog(dialog);

        List<QuizQuestion> questions = quiz.getQuestions();
        if (questions == null) {
            quiz.setQuestions(new ArrayList<>());
            return;
        }

        for (QuizQuestion question : questions) {
            question.setId(null);
            question.setQuiz(quiz);
        }
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
