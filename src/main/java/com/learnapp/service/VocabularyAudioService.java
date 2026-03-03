package com.learnapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnapp.config.TextToSpeechProperties;
import com.learnapp.dto.VocabularyAudioBackfillResponse;
import com.learnapp.dto.VocabularyAudioResponse;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.entities.VocabularyAudio;
import com.learnapp.error.AppException;
import com.learnapp.repository.VocabularyAudioRepository;
import com.learnapp.repository.VocabularyRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class VocabularyAudioService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyAudioService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final VocabularyAudioRepository vocabularyAudioRepository;
    private final VocabularyRepository vocabularyRepository;
    private final TextToSpeechProperties textToSpeechProperties;
    private final VocabularyAudioStorageService vocabularyAudioStorageService;
    private final Object requestLock = new Object();
    private Instant nextAllowedRequestAt = Instant.EPOCH;
    private Instant cooldownUntil = Instant.EPOCH;

    public VocabularyAudioService(
            VocabularyAudioRepository vocabularyAudioRepository,
            VocabularyRepository vocabularyRepository,
            TextToSpeechProperties textToSpeechProperties,
            VocabularyAudioStorageService vocabularyAudioStorageService
    ) {
        this.vocabularyAudioRepository = vocabularyAudioRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.textToSpeechProperties = textToSpeechProperties;
        this.vocabularyAudioStorageService = vocabularyAudioStorageService;
    }

    public void populateAudios(Vocabulary vocabulary) {
        replaceAudios(vocabulary, false);
    }

    public void refreshAudios(Vocabulary vocabulary) {
        replaceAudios(vocabulary, true);
    }

    public VocabularyAudioResponse addUploadedAudio(Vocabulary vocabulary, MultipartFile file, String accent) {
        if (vocabulary == null || vocabulary.getId() == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found");
        }

        List<VocabularyAudio> existingAudios =
                vocabularyAudioRepository.findByVocabularyIdOrderByPositionAscCreatedAtAsc(vocabulary.getId());
        int nextPosition = existingAudios.size() + 1;
        String normalizedAccent = normalizeAccent(accent);
        String storedAudioUrl = vocabularyAudioStorageService.uploadVocabularyAudio(
                vocabulary.getId(),
                file,
                nextPosition,
                normalizedAccent
        );

        try {
            VocabularyAudio audio = vocabularyAudioRepository.save(VocabularyAudio.builder()
                    .vocabularyId(vocabulary.getId())
                    .audioUrl(storedAudioUrl)
                    .accent(normalizedAccent)
                    .position(nextPosition)
                    .build());
            return toResponse(audio);
        } catch (RuntimeException ex) {
            vocabularyAudioStorageService.deleteAudioByUrl(storedAudioUrl);
            throw ex;
        }
    }

    public void deleteAudio(UUID vocabularyId, UUID audioId) {
        VocabularyAudio audio = vocabularyAudioRepository.findByIdAndVocabularyId(audioId, vocabularyId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_AUDIO_NOT_FOUND", "Vocabulary audio not found"));
        vocabularyAudioRepository.delete(audio);
        vocabularyAudioStorageService.deleteAudioByUrl(audio.getAudioUrl());
        resequencePositions(vocabularyId);
    }

    public VocabularyAudioBackfillResponse backfillAudios(
            String language,
            VocabularyStatus status,
            boolean forceRefresh,
            Integer batchSize,
            Integer limit
    ) {
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage == null) {
            normalizedLanguage = "en";
        }
        if (!supportsAudioLookup(normalizedLanguage)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_AUDIO_LANGUAGE", "Audio lookup is only supported for English vocabulary");
        }

        int effectiveBatchSize = batchSize == null ? 100 : batchSize;
        if (effectiveBatchSize < 1 || effectiveBatchSize > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_BATCH_SIZE", "Batch size must be between 1 and 500");
        }
        if (limit != null && limit < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LIMIT", "Limit must be greater than 0");
        }

        long processed = 0;
        long updated = 0;
        long skipped = 0;
        long failed = 0;
        int pageNumber = 0;
        boolean withoutAudioOnly = !forceRefresh;
        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

        while (limit == null || processed < limit) {
            int remaining = limit == null ? effectiveBatchSize : (int) Math.min(effectiveBatchSize, (long) limit - processed);
            PageRequest pageable = PageRequest.of(withoutAudioOnly ? 0 : pageNumber, remaining, sort);
            Page<Vocabulary> page = vocabularyRepository.searchForAudioBackfill(
                    normalizedLanguage,
                    status,
                    withoutAudioOnly,
                    pageable
            );
            if (page.isEmpty()) {
                break;
            }

            for (Vocabulary vocabulary : page.getContent()) {
                AudioSyncOutcome outcome = forceRefresh ? replaceAudios(vocabulary, true) : replaceAudios(vocabulary, false);
                processed++;
                if (outcome == AudioSyncOutcome.UPDATED) {
                    updated++;
                } else if (outcome == AudioSyncOutcome.RATE_LIMITED) {
                    failed++;
                    break;
                } else if (outcome == AudioSyncOutcome.FAILED) {
                    failed++;
                } else {
                    skipped++;
                }
                if (limit != null && processed >= limit) {
                    break;
                }
            }

            if (isInCooldown()) {
                break;
            }
            if (!withoutAudioOnly) {
                pageNumber++;
            }
        }

        return new VocabularyAudioBackfillResponse(
                normalizedLanguage,
                status,
                forceRefresh,
                effectiveBatchSize,
                limit,
                processed,
                updated,
                skipped,
                failed
        );
    }

    @Transactional(readOnly = true)
    public List<VocabularyAudioResponse> loadAudioResponses(UUID vocabularyId) {
        return toResponses(vocabularyAudioRepository.findByVocabularyIdOrderByPositionAscCreatedAtAsc(vocabularyId));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<VocabularyAudioResponse>> loadAudioResponses(List<UUID> vocabularyIds) {
        Map<UUID, List<VocabularyAudioResponse>> result = new HashMap<>();
        if (vocabularyIds == null || vocabularyIds.isEmpty()) {
            return result;
        }

        List<VocabularyAudio> audios =
                vocabularyAudioRepository.findByVocabularyIdInOrderByVocabularyIdAscPositionAscCreatedAtAsc(vocabularyIds);
        for (VocabularyAudio audio : audios) {
            result.computeIfAbsent(audio.getVocabularyId(), key -> new ArrayList<>())
                    .add(toResponse(audio));
        }
        return result;
    }

    private AudioSyncOutcome replaceAudios(Vocabulary vocabulary, boolean clearExistingOnRefresh) {
        if (vocabulary == null || vocabulary.getId() == null || !textToSpeechProperties.isEnabled()) {
            return AudioSyncOutcome.SKIPPED;
        }

        String term = trimToNull(vocabulary.getTerm());
        String language = normalizeLanguage(vocabulary.getLanguage());
        if (term == null || language == null) {
            return AudioSyncOutcome.SKIPPED;
        }

        if (!supportsAudioLookup(language)) {
            if (clearExistingOnRefresh) {
                deleteExistingAudios(vocabulary.getId());
            }
            return AudioSyncOutcome.SKIPPED;
        }

        List<AudioCandidate> candidates = synthesizeAudioCandidates(language, term);
        if (candidates == null) {
            if (isInCooldown()) {
                return AudioSyncOutcome.RATE_LIMITED;
            }
            return AudioSyncOutcome.FAILED;
        }

        List<VocabularyAudio> existingAudios =
                vocabularyAudioRepository.findByVocabularyIdOrderByPositionAscCreatedAtAsc(vocabulary.getId());
        if (candidates.isEmpty()) {
            deleteExistingAudios(vocabulary.getId(), existingAudios);
            return AudioSyncOutcome.SKIPPED;
        }

        List<VocabularyAudio> audios = new ArrayList<>();
        List<String> uploadedUrls = new ArrayList<>();
        int position = 1;
        try {
            for (AudioCandidate candidate : candidates) {
                String storedAudioUrl = vocabularyAudioStorageService.uploadVocabularyAudio(
                        vocabulary.getId(),
                        candidate.audioBytes(),
                        candidate.contentType(),
                        "tts-" + vocabulary.getId() + "-" + position + ".wav",
                        position,
                        candidate.accent()
                );
                uploadedUrls.add(storedAudioUrl);
                audios.add(VocabularyAudio.builder()
                        .vocabularyId(vocabulary.getId())
                        .audioUrl(storedAudioUrl)
                        .accent(candidate.accent())
                        .position(position++)
                        .build());
            }
        } catch (RuntimeException ex) {
            cleanupUploadedAudios(uploadedUrls);
            log.warn(
                    "Failed to store vocabulary audio in MinIO for vocabularyId={}, term='{}': {}",
                    vocabulary.getId(),
                    term,
                    ex.getMessage()
            );
            return AudioSyncOutcome.FAILED;
        }

        try {
            vocabularyAudioRepository.deleteByVocabularyId(vocabulary.getId());
            vocabularyAudioRepository.saveAll(audios);
        } catch (RuntimeException ex) {
            cleanupUploadedAudios(uploadedUrls);
            log.warn(
                    "Failed to persist vocabulary audio records for vocabularyId={}, term='{}': {}",
                    vocabulary.getId(),
                    term,
                    ex.getMessage()
            );
            return AudioSyncOutcome.FAILED;
        }
        deleteStoredAudioObjects(existingAudios);
        return AudioSyncOutcome.UPDATED;
    }

    private List<AudioCandidate> synthesizeAudioCandidates(String language, String term) {
        synchronized (requestLock) {
            if (isInCooldownUnsafe()) {
                log.warn(
                        "TTS server is in cooldown until {}. Skip synthesis for term '{}' and language '{}'",
                        cooldownUntil,
                        term,
                        language
                );
                return null;
            }

            waitForNextAllowedRequest();
            try {
                String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of("text", term));
                HttpURLConnection connection = (HttpURLConnection) resolveTtsUri().toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "audio/wav, application/octet-stream");
                byte[] requestBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(requestBytes.length);
                connection.getOutputStream().write(requestBytes);
                int statusCode = connection.getResponseCode();
                if (statusCode == 200) {
                    markRequestCompleted();
                    byte[] audioBytes = readAllBytes(connection.getInputStream());
                    if (audioBytes == null || audioBytes.length == 0) {
                        return List.of();
                    }
                    return List.of(new AudioCandidate(
                            audioBytes,
                            "audio/wav",
                            defaultAccent()
                    ));
                }
                if (statusCode == 404) {
                    markRequestCompleted();
                    return List.of();
                }
                if (statusCode == 429) {
                    markRateLimited();
                    log.warn(
                            "TTS server returned 429 TOO_MANY_REQUESTS for term '{}' and language '{}'. Cooldown until {}",
                            term,
                            language,
                            cooldownUntil
                        );
                    return null;
                }
                markRequestCompleted();
                String errorBody = readErrorBody(connection);
                log.warn("TTS server returned {} for term '{}' and language '{}'", statusCode, term, language);
                if (errorBody != null) {
                    log.warn("TTS server error body for term '{}': {}", term, errorBody);
                }
                return null;
            } catch (JsonProcessingException ex) {
                markRequestCompleted();
                log.warn("Cannot serialize TTS request for term '{}' and language '{}': {}", term, language, ex.getMessage());
                return null;
            } catch (Exception ex) {
                markRequestCompleted();
                log.warn("TTS synthesis failed for term '{}' and language '{}': {}", term, language, ex.getMessage());
                return null;
            }
        }
    }

    private boolean isInCooldown() {
        synchronized (requestLock) {
            return isInCooldownUnsafe();
        }
    }

    private boolean isInCooldownUnsafe() {
        return cooldownUntil.isAfter(Instant.now());
    }

    private void waitForNextAllowedRequest() {
        long waitMs = nextAllowedRequestAt.toEpochMilli() - Instant.now().toEpochMilli();
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_FETCH_INTERRUPTED", "Audio fetch was interrupted");
        }
    }

    private void markRequestCompleted() {
        long intervalMs = Math.max(0, textToSpeechProperties.getMinRequestIntervalMs());
        nextAllowedRequestAt = Instant.now().plusMillis(intervalMs);
    }

    private void markRateLimited() {
        long cooldownSeconds = Math.max(1, textToSpeechProperties.getCooldownSeconds());
        cooldownUntil = Instant.now().plusSeconds(cooldownSeconds);
        nextAllowedRequestAt = cooldownUntil;
    }

    private void deleteExistingAudios(UUID vocabularyId) {
        deleteExistingAudios(
                vocabularyId,
                vocabularyAudioRepository.findByVocabularyIdOrderByPositionAscCreatedAtAsc(vocabularyId)
        );
    }

    private void deleteExistingAudios(UUID vocabularyId, List<VocabularyAudio> existingAudios) {
        vocabularyAudioRepository.deleteByVocabularyId(vocabularyId);
        deleteStoredAudioObjects(existingAudios);
    }

    private void deleteStoredAudioObjects(List<VocabularyAudio> existingAudios) {
        if (existingAudios == null || existingAudios.isEmpty()) {
            return;
        }
        for (VocabularyAudio existingAudio : existingAudios) {
            vocabularyAudioStorageService.deleteAudioByUrl(existingAudio.getAudioUrl());
        }
    }

    private void cleanupUploadedAudios(List<String> uploadedUrls) {
        for (String uploadedUrl : uploadedUrls) {
            vocabularyAudioStorageService.deleteAudioByUrl(uploadedUrl);
        }
    }

    private List<VocabularyAudioResponse> toResponses(List<VocabularyAudio> audios) {
        List<VocabularyAudioResponse> responses = new ArrayList<>();
        for (VocabularyAudio audio : audios) {
            responses.add(toResponse(audio));
        }
        return responses;
    }

    private VocabularyAudioResponse toResponse(VocabularyAudio audio) {
        return new VocabularyAudioResponse(
                audio.getId(),
                audio.getAudioUrl(),
                audio.getAccent(),
                audio.getPosition()
        );
    }

    private boolean supportsAudioLookup(String language) {
        return "en".equals(language);
    }

    private String normalizeLanguage(String language) {
        String normalized = trimToNull(language);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeAccent(String accent) {
        String normalized = trimToNull(accent);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private void resequencePositions(UUID vocabularyId) {
        List<VocabularyAudio> remainingAudios =
                vocabularyAudioRepository.findByVocabularyIdOrderByPositionAscCreatedAtAsc(vocabularyId);
        int position = 1;
        boolean changed = false;
        for (VocabularyAudio audio : remainingAudios) {
            if (audio.getPosition() == null || audio.getPosition() != position) {
                audio.setPosition(position);
                changed = true;
            }
            position++;
        }
        if (changed) {
            vocabularyAudioRepository.saveAll(remainingAudios);
        }
    }

    private String defaultAccent() {
        String normalizedAccent = normalizeAccent(textToSpeechProperties.getDefaultAccent());
        return normalizedAccent == null ? "us" : normalizedAccent;
    }

    private URI resolveTtsUri() {
        String baseUrl = textToSpeechProperties.getBaseUrl();
        String path = textToSpeechProperties.getSynthesizePath();
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return URI.create(baseUrl.substring(0, baseUrl.length() - 1) + path);
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return URI.create(baseUrl + "/" + path);
        }
        return URI.create(baseUrl + path);
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        try (InputStream stream = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private String readErrorBody(HttpURLConnection connection) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream == null) {
                return null;
            }
            return new String(readAllBytes(errorStream), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record AudioCandidate(byte[] audioBytes, String contentType, String accent) {
    }

    private enum AudioSyncOutcome {
        UPDATED,
        SKIPPED,
        FAILED,
        RATE_LIMITED
    }
}
