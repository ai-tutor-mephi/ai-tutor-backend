package com.VLmb.ai_tutor_backend.feature.quiz.infra;

import com.VLmb.ai_tutor_backend.feature.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findByIdAndDialogId(Long id, Long dialogId);

    @EntityGraph(attributePaths = {"questions", "dialog", "dialog.owner"})
    @Query("select quiz from Quiz quiz where quiz.id = :quizId")
    Optional<Quiz> findByIdWithQuestions(@Param("quizId") Long quizId);

    @EntityGraph(attributePaths = "questions")
    List<Quiz> findByDialogIdOrderByCreatedAtDesc(Long dialogId);
}
