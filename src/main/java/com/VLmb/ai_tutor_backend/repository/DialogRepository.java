package com.VLmb.ai_tutor_backend.repository;

import com.VLmb.ai_tutor_backend.entity.Dialog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, Long> {
    List<Dialog> findByOwnerId(Long userId);
}
