package com.VLmb.ai_tutor_backend.repository;

import com.VLmb.ai_tutor_backend.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.File;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

}
