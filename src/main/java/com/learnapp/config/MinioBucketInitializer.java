package com.learnapp.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MinioBucketInitializer {

    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioBucketInitializer(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBucket() {
        String bucket = minioProperties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            log.warn("MinIO bucket is empty. Skip initialization.");
            return;
        }

        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucket)
                            .config(MinioBucketPolicySupport.buildPublicReadPolicy(bucket))
                            .build()
            );
            log.info("Applied public-read policy to MinIO bucket: {}", bucket);
        } catch (Exception ex) {
            log.warn("Cannot initialize MinIO bucket '{}': {}", bucket, ex.getMessage());
        }
    }
}
