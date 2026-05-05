package com.VLmb.ai_tutor_backend.feature.file.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Service
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        ensureBucketExists();

        log.debug("event=s3_upload_start object_key={} size={}", objectKey, contentLength);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        log.debug("event=s3_upload_success object_key={} size={}", objectKey, contentLength);
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
            } catch (S3Exception ex) {
                throw new RuntimeException("Could not create bucket: " + bucketName, ex);
            }
        }
    }

    public void deleteFile(String objectKey) {
        log.debug("event=s3_delete_start object_key={}", objectKey);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
        log.debug("event=s3_delete_success object_key={}", objectKey);
    }

}
