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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
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
public class VocabularyAudioStorageService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyAudioStorageService.class);
    private static final long MAX_AUDIO_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "application/ogg",
            "audio/webm"
    );
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "audio/mpeg", "mp3",
            "audio/mp3", "mp3",
            "audio/wav", "wav",
            "audio/x-wav", "wav",
            "audio/ogg", "ogg",
            "application/ogg", "ogg",
            "audio/webm", "webm"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "ogg", "webm");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public VocabularyAudioStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public String uploadVocabularyAudio(UUID vocabularyId, String sourceUrl, Integer position, String accent) {
        ensureBucketExists();
        RemoteAudioFile remoteAudioFile = downloadRemoteAudio(sourceUrl);
        return uploadVocabularyAudio(vocabularyId, remoteAudioFile, sourceUrl, position, accent);
    }

    public String uploadVocabularyAudio(
            UUID vocabularyId,
            byte[] bytes,
            String contentType,
            String sourceReference,
            Integer position,
            String accent
    ) {
        ensureBucketExists();
        RemoteAudioFile remoteAudioFile = fromBytes(bytes, contentType, sourceReference);
        return uploadVocabularyAudio(vocabularyId, remoteAudioFile, sourceReference, position, accent);
    }

    public String uploadVocabularyAudio(UUID vocabularyId, MultipartFile file, Integer position, String accent) {
        ensureBucketExists();
        RemoteAudioFile remoteAudioFile = fromMultipartFile(file);
        String sourceName = file == null ? null : file.getOriginalFilename();
        return uploadVocabularyAudio(vocabularyId, remoteAudioFile, sourceName, position, accent);
    }

    private String uploadVocabularyAudio(
            UUID vocabularyId,
            RemoteAudioFile remoteAudioFile,
            String sourceReference,
            Integer position,
            String accent
    ) {
        String extension = resolveExtension(sourceReference, remoteAudioFile.contentType());
        String objectName = buildObjectName(vocabularyId, position, accent, extension);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(remoteAudioFile.bytes())) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(inputStream, remoteAudioFile.bytes().length, -1)
                            .contentType(remoteAudioFile.contentType())
                            .build()
            );
            return buildObjectUrl(objectName);
        } catch (Exception ex) {
            log.error(
                    "Failed to upload vocabulary audio: vocabularyId={}, sourceUrl={}, reason={}",
                    vocabularyId,
                    sourceReference,
                    ex.getMessage()
            );
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "VOCABULARY_AUDIO_UPLOAD_FAILED",
                    "Cannot upload vocabulary audio"
            );
        }
    }

    private RemoteAudioFile fromMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Audio file is required");
        }
        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "AUDIO_FILE_TOO_LARGE",
                    "Vocabulary audio must not exceed 10MB"
            );
        }

        try {
            String contentType = resolveContentType(file.getOriginalFilename(), normalizeContentType(file.getContentType()));
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", "Vocabulary audio is empty");
            }
            return new RemoteAudioFile(bytes, contentType);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to read uploaded vocabulary audio: filename={}, reason={}", file.getOriginalFilename(), ex.getMessage());
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AUDIO_FILE",
                    "Cannot read vocabulary audio file"
            );
        }
    }

    private RemoteAudioFile fromBytes(byte[] bytes, String contentType, String sourceReference) {
        if (bytes == null || bytes.length == 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", "Vocabulary audio is empty");
        }
        if (bytes.length > MAX_AUDIO_SIZE_BYTES) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "AUDIO_FILE_TOO_LARGE",
                    "Vocabulary audio must not exceed 10MB"
            );
        }
        String resolvedContentType = resolveContentType(sourceReference, normalizeContentType(contentType));
        return new RemoteAudioFile(bytes, resolvedContentType);
    }

    public void deleteAudioByUrl(String audioUrl) {
        String objectName = resolveObjectNameFromUrl(audioUrl);
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
            log.warn("Cannot remove vocabulary audio object: object={}, reason={}", objectName, ex.getMessage());
        }
    }

    private RemoteAudioFile downloadRemoteAudio(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_URL", "Audio source URL is required");
        }

        try {
            URLConnection connection = URI.create(sourceUrl).toURL().openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);

            String contentType = normalizeContentType(connection.getContentType());
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_AUDIO_SIZE_BYTES) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "AUDIO_FILE_TOO_LARGE",
                        "Vocabulary audio must not exceed 10MB"
                );
            }

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = readBytes(inputStream);
                if (bytes.length == 0) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", "Vocabulary audio is empty");
                }

                String resolvedContentType = resolveContentType(sourceUrl, contentType);
                return new RemoteAudioFile(bytes, resolvedContentType);
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to download remote vocabulary audio: sourceUrl={}, reason={}", sourceUrl, ex.getMessage());
            throw new AppException(
                    HttpStatus.BAD_GATEWAY,
                    "VOCABULARY_AUDIO_DOWNLOAD_FAILED",
                    "Cannot download vocabulary audio"
            );
        }
    }

    private byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalRead += bytesRead;
            if (totalRead > MAX_AUDIO_SIZE_BYTES) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "AUDIO_FILE_TOO_LARGE",
                        "Vocabulary audio must not exceed 10MB"
                );
            }
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
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
                    "Cannot access MinIO bucket for vocabulary audio: endpoint={}, bucket={}, reason={}",
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

    private String buildObjectName(UUID vocabularyId, Integer position, String accent, String extension) {
        String normalizedAccent = normalizePathSegment(accent);
        String positionPrefix = position == null ? "0" : position.toString();
        String accentPrefix = normalizedAccent == null ? "audio" : normalizedAccent;
        return "vocabulary-audios/" + vocabularyId + "/" + positionPrefix + "-" + accentPrefix + "-" + UUID.randomUUID() + "." + extension;
    }

    private String buildObjectUrl(String objectName) {
        String baseUrl = trimToNull(minioProperties.getPublicUrl());
        if (baseUrl == null) {
            baseUrl = trimToNull(minioProperties.getUrl());
        }
        if (baseUrl == null) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MINIO_CONFIG_INVALID",
                    "MinIO URL is not configured"
            );
        }

        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/" + minioProperties.getBucket() + "/" + objectName;
    }

    private String resolveObjectNameFromUrl(String audioUrl) {
        String bucket = minioProperties.getBucket();
        if (audioUrl == null || audioUrl.isBlank() || bucket == null || bucket.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(audioUrl);
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

    private String resolveContentType(String sourceUrl, String contentType) {
        if (ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return contentType;
        }

        String extension = extensionFromUrl(sourceUrl);
        if ("mp3".equals(extension)) {
            return "audio/mpeg";
        }
        if ("wav".equals(extension)) {
            return "audio/wav";
        }
        if ("ogg".equals(extension)) {
            return "audio/ogg";
        }
        if ("webm".equals(extension)) {
            return "audio/webm";
        }

        throw new AppException(
                HttpStatus.BAD_REQUEST,
                "INVALID_AUDIO_FILE_TYPE",
                "Only MP3, WAV, OGG, WEBM audio files are allowed"
        );
    }

    private String resolveExtension(String sourceUrl, String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension != null) {
            return extension;
        }

        String fromUrl = extensionFromUrl(sourceUrl);
        if (fromUrl != null) {
            return fromUrl;
        }
        return "bin";
    }

    private String extensionFromUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        try {
            String path = URI.create(sourceUrl).getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0 || lastDot == path.length() - 1) {
                return null;
            }
            String extension = path.substring(lastDot + 1).toLowerCase(Locale.ROOT);
            return ALLOWED_EXTENSIONS.contains(extension) ? extension : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }
        if ("audio/mpga".equals(normalized)) {
            return "audio/mpeg";
        }
        return normalized;
    }

    private String normalizePathSegment(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RemoteAudioFile(byte[] bytes, String contentType) {
    }
}
