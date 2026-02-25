package com.VLmb.ai_tutor_backend.feature.file.infra;

import com.VLmb.ai_tutor_backend.feature.file.domain.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.File;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
}
