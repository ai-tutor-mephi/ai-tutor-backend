package com.VLmb.ai_tutor_backend.feature.dialog.infra;

import com.VLmb.ai_tutor_backend.feature.dialog.domain.Dialog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, Long> {
    List<Dialog> findByOwnerId(Long userId);

    List<Dialog> findByOwnerIdOrderByCreatedAtDesc(Long id);
}
