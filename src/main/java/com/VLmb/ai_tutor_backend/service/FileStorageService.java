package com.VLmb.ai_tutor_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Service
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    public void deleteFile(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }

}
