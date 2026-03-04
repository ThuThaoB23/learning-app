package com.learnapp.service;

import com.learnapp.config.MinioBucketPolicySupport;
import com.learnapp.config.MinioProperties;
import com.learnapp.error.AppException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public AvatarStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public String uploadUserAvatar(UUID userId, MultipartFile file) {
        validateAvatar(file);
        ensureBucketExists();

        String contentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String objectName = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            return buildObjectUrl(objectName);
        } catch (Exception ex) {
            log.error("Failed to upload avatar: userId={}, reason={}", userId, ex.getMessage());
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AVATAR_UPLOAD_FAILED",
                    "Cannot upload avatar image"
            );
        }
    }

    public void deleteAvatarByUrl(String avatarUrl) {
        String objectName = resolveObjectNameFromUrl(avatarUrl);
        if (objectName == null) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception ex) {
            log.warn("Cannot remove old avatar object: object={}, reason={}", objectName, ex.getMessage());
        }
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Avatar image is required");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Avatar image must not exceed 5MB");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILE_TYPE",
                    "Only JPG, PNG, WEBP, GIF images are allowed"
            );
        }
    }

    private void ensureBucketExists() {
        String bucket = minioProperties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MINIO_CONFIG_INVALID",
                    "MinIO bucket is not configured"
            );
        }

        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucket)
                            .config(MinioBucketPolicySupport.buildPublicReadPolicy(bucket))
                            .build()
            );
        } catch (Exception ex) {
            log.error(
                    "Cannot access MinIO bucket: endpoint={}, bucket={}, reason={}",
                    minioProperties.getUrl(),
                    bucket,
                    ex.getMessage()
            );
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MINIO_UNAVAILABLE",
                    "MinIO is unavailable"
            );
        }
    }

    private String buildObjectUrl(String objectName) {
        String baseUrl = minioProperties.getObjectBaseUrl();
        if (baseUrl == null) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MINIO_CONFIG_INVALID",
                    "MinIO URL is not configured"
            );
        }

        return baseUrl + "/" + minioProperties.getBucket() + "/" + objectName;
    }

    private String resolveObjectNameFromUrl(String avatarUrl) {
        String bucket = minioProperties.getBucket();
        if (avatarUrl == null || avatarUrl.isBlank() || bucket == null || bucket.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(avatarUrl);
            String path = uri.getPath();
            String prefix = "/" + bucket + "/";
            if (path == null || !path.startsWith(prefix)) {
                return null;
            }
            return path.substring(prefix.length());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String fromName = extensionFromFilename(originalFilename);
        if (fromName != null) {
            return fromName;
        }
        String fromContentType = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (fromContentType != null) {
            return fromContentType;
        }
        return "bin";
    }

    private String extensionFromFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return null;
        }
        String extension = originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension) ? ("jpeg".equals(extension) ? "jpg" : extension) : null;
    }
}
