package com.VLmb.ai_tutor_backend.repository;

import com.VLmb.ai_tutor_backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByDialogId(Long dialogId, Pageable pageable);
    List<Message> findByDialogId(Long dialogId);
}
