package com.VLmb.ai_tutor_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "files")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_file_name", nullable = false, unique = true)
    private String storageFileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "upload_date", nullable = false, updatable = false)
    private OffsetDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialog_id", nullable = false)
    private Dialog dialog;

    @PrePersist
    public void onPrePersist() {
        this.uploadDate = OffsetDateTime.now();
    }
}
