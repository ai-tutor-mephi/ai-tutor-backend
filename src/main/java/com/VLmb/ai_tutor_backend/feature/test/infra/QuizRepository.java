package com.VLmb.ai_tutor_backend.feature.test.infra;

import com.VLmb.ai_tutor_backend.feature.test.domain.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = "questions")
    Optional<Quiz> findByIdAndDialogId(Long id, Long dialogId);

    @EntityGraph(attributePaths = "questions")
    List<Quiz> findByDialogIdOrderByCreatedAtDesc(Long dialogId);
}
