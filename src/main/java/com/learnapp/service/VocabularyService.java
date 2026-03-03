package com.learnapp.service;

import com.learnapp.dto.UpdateVocabularyExampleRequest;
import com.learnapp.dto.UpdateVocabularyRequest;
import com.learnapp.dto.VocabularyAudioResponse;
import com.learnapp.dto.VocabularyDetailResponse;
import com.learnapp.dto.VocabularyImportErrorResponse;
import com.learnapp.dto.VocabularyImportResultResponse;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.TopicVocabulary;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyExample;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyExampleRepository;
import com.learnapp.repository.VocabularyRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class VocabularyService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final TopicVocabularyRepository topicVocabularyRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final VocabularyExampleRepository vocabularyExampleRepository;
    private final VocabularyAudioService vocabularyAudioService;

    public VocabularyService(
            VocabularyRepository vocabularyRepository,
            TopicRepository topicRepository,
            TopicVocabularyRepository topicVocabularyRepository,
            UserVocabularyRepository userVocabularyRepository,
            VocabularyExampleRepository vocabularyExampleRepository,
            VocabularyAudioService vocabularyAudioService
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.topicRepository = topicRepository;
        this.topicVocabularyRepository = topicVocabularyRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.vocabularyExampleRepository = vocabularyExampleRepository;
        this.vocabularyAudioService = vocabularyAudioService;
    }

    @Transactional(readOnly = true)
    public Page<VocabularyResponse> searchApproved(
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            Pageable pageable
    ) {
        return toResponses(searchApprovedPage(query, topicId, language, status, pageable));
    }

    @Transactional(readOnly = true)
    public Page<VocabularyResponse> searchApprovedNotAddedByUser(
            UUID userId,
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            Pageable pageable
    ) {
        String normalizedQuery = normalizeTerm(query);
        String normalizedLanguage = normalizeLanguage(language);
        VocabularyStatus effectiveStatus = status == null ? VocabularyStatus.APPROVED : status;

        if (topicId != null) {
            return toResponses(vocabularyRepository.searchByTopicNotAddedByUser(
                    userId,
                    topicId,
                    effectiveStatus,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            ));
        }

        return toResponses(vocabularyRepository.searchApprovedNotAddedByUser(
                userId,
                effectiveStatus,
                normalizedLanguage,
                normalizedQuery,
                pageable
        ));
    }

    @Transactional(readOnly = true)
    public Page<VocabularyResponse> searchForUser(
            UUID userId,
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            boolean includeMyVocab,
            Pageable pageable
    ) {
        VocabularyStatus effectiveStatus = status == null ? VocabularyStatus.APPROVED : status;
        if (includeMyVocab) {
            return toResponses(searchApprovedPage(query, topicId, language, effectiveStatus, pageable), userId);
        }
        return searchApprovedNotAddedByUser(userId, query, topicId, language, effectiveStatus, pageable)
                .map(vocab -> withInMyVocab(vocab, false));
    }

    private Page<Vocabulary> searchApprovedPage(
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            Pageable pageable
    ) {
        String normalizedQuery = normalizeTerm(query);
        String normalizedLanguage = normalizeLanguage(language);

        if (topicId != null) {
            return vocabularyRepository.searchByTopic(
                    topicId,
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedQuery != null && normalizedLanguage != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedQuery != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedLanguage != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguage(
                    status,
                    normalizedLanguage,
                    pageable
            );
        }

        if (status != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        }

        if (normalizedQuery != null && normalizedLanguage != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (normalizedQuery != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    normalizedQuery,
                    pageable
            );
        }

        if (normalizedLanguage != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndLanguage(
                    normalizedLanguage,
                    pageable
            );
        }

        return vocabularyRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponse> exportVocabularies(
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status
    ) {
        return searchApproved(query, topicId, language, status, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public VocabularyResponse getApproved(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndStatusAndDeletedAtIsNull(id, VocabularyStatus.APPROVED)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    @Transactional(readOnly = true)
    public VocabularyDetailResponse getDetail(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        List<String> examples = loadExamples(vocabulary.getId());
        List<VocabularyAudioResponse> audios = vocabularyAudioService.loadAudioResponses(vocabulary.getId());
        List<UUID> topicIds = topicVocabularyRepository.findByVocabularyId(vocabulary.getId()).stream()
                .map(TopicVocabulary::getTopicId)
                .distinct()
                .toList();
        return toDetailResponse(vocabulary, examples, audios, topicIds);
    }

    public VocabularyResponse createContribution(
            UUID userId,
            String term,
            String definition,
            String definitionVi,
            List<String> examples,
            String phonetic,
            String partOfSpeech,
            String language,
            List<UUID> topicIds
    ) {
        String normalizedTerm = normalizeTerm(term);
        if (normalizedTerm == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
        }
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
        }
        if (definition == null || definition.trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEFINITION", "Definition is required");
        }

        vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(normalizedTerm, normalizedLanguage)
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
                });

        Vocabulary vocabulary = Vocabulary.builder()
                .term(term.trim())
                .termNormalized(normalizedTerm)
                .definition(definition.trim())
                .definitionVi(trimToNull(definitionVi))
                .phonetic(phonetic == null ? null : phonetic.trim())
                .partOfSpeech(partOfSpeech == null ? null : partOfSpeech.trim())
                .language(normalizedLanguage)
                .status(VocabularyStatus.PENDING)
                .createdBy(userId)
                .build();

        vocabulary = vocabularyRepository.save(vocabulary);

        if (examples != null && !examples.isEmpty()) {
            List<VocabularyExample> exampleEntities = new ArrayList<>();
            for (String example : examples) {
                if (example == null || example.trim().isEmpty()) {
                    continue;
                }
                exampleEntities.add(VocabularyExample.builder()
                        .vocabularyId(vocabulary.getId())
                        .example(example.trim())
                        .build());
            }
            if (!exampleEntities.isEmpty()) {
                vocabularyExampleRepository.saveAll(exampleEntities);
            }
        }

        if (topicIds != null && !topicIds.isEmpty()) {
            List<TopicVocabulary> links = buildTopicLinks(vocabulary.getId(), topicIds);
            topicVocabularyRepository.saveAll(links);
        }

        vocabularyAudioService.populateAudios(vocabulary);

        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public VocabularyImportResultResponse importFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "CSV file is required");
        }
        log.info("Start CSV import: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        List<VocabularyImportErrorResponse> errors = new ArrayList<>();
        int totalRows = 0;
        int importedRows = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            log.info("CSV headers detected: {}", parser.getHeaderMap().keySet());
            for (CSVRecord record : parser) {
                totalRows++;
                log.info("CSV row {} raw: {}", record.getRecordNumber(), record.toMap());
                try {
                    importRecord(record);
                    importedRows++;
                    log.info("CSV row {} imported successfully", record.getRecordNumber());
                } catch (AppException | IllegalArgumentException ex) {
                    log.error("CSV row {} import failed: {}", record.getRecordNumber(), ex.getMessage());
                    errors.add(new VocabularyImportErrorResponse(record.getRecordNumber(), ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            log.error("CSV import failed while reading file: {}", ex.getMessage());
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Cannot read CSV file");
        }

        log.info(
                "CSV import completed: totalRows={}, importedRows={}, failedRows={}",
                totalRows,
                importedRows,
                totalRows - importedRows
        );
        return new VocabularyImportResultResponse(totalRows, importedRows, totalRows - importedRows, errors);
    }

    public VocabularyResponse approve(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.APPROVED);
        vocabulary = vocabularyRepository.save(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public VocabularyResponse reject(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.REJECTED);
        vocabulary = vocabularyRepository.save(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public VocabularyResponse refreshVocabularyAudio(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabularyAudioService.refreshAudios(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public VocabularyResponse uploadVocabularyAudio(UUID id, MultipartFile file, String accent) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndStatusAndDeletedAtIsNull(id, VocabularyStatus.APPROVED)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabularyAudioService.addUploadedAudio(vocabulary, file, accent);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public void deleteVocabularyAudio(UUID vocabularyId, UUID audioId) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(vocabularyId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabularyAudioService.deleteAudio(vocabulary.getId(), audioId);
    }

    public VocabularyResponse updateVocabulary(UUID id, UpdateVocabularyRequest request) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));

        String updatedTermNormalized = vocabulary.getTermNormalized();
        String updatedLanguage = vocabulary.getLanguage();
        boolean shouldRefreshAudios = false;

        if (request.term() != null) {
            updatedTermNormalized = normalizeTerm(request.term());
            if (updatedTermNormalized == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
            }
            shouldRefreshAudios = true;
            vocabulary.setTerm(request.term().trim());
            vocabulary.setTermNormalized(updatedTermNormalized);
        }

        if (request.language() != null) {
            updatedLanguage = normalizeLanguage(request.language());
            if (updatedLanguage == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
            }
            shouldRefreshAudios = true;
            vocabulary.setLanguage(updatedLanguage);
        }

        if (request.term() != null || request.language() != null) {
            Vocabulary existing = vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(
                    updatedTermNormalized,
                    updatedLanguage
            ).orElse(null);
            if (existing != null && !existing.getId().equals(vocabulary.getId())) {
                throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
            }
        }

        if (request.definition() != null) {
            String definition = request.definition().trim();
            if (definition.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEFINITION", "Definition is required");
            }
            vocabulary.setDefinition(definition);
        }
        if (request.definitionVi() != null) {
            vocabulary.setDefinitionVi(trimToNull(request.definitionVi()));
        }
        if (request.phonetic() != null) {
            vocabulary.setPhonetic(trimToNull(request.phonetic()));
        }
        if (request.partOfSpeech() != null) {
            vocabulary.setPartOfSpeech(trimToNull(request.partOfSpeech()));
        }
        if (request.status() != null) {
            vocabulary.setStatus(request.status());
        }

        vocabulary = vocabularyRepository.save(vocabulary);

        if (request.examples() != null) {
            syncExamples(vocabulary.getId(), request.examples());
        }
        if (request.topicIds() != null) {
            syncTopicLinks(vocabulary.getId(), request.topicIds());
        }

        if (shouldRefreshAudios) {
            vocabularyAudioService.refreshAudios(vocabulary);
        }

        return toResponse(vocabulary, loadExamples(vocabulary.getId()), vocabularyAudioService.loadAudioResponses(vocabulary.getId()));
    }

    public void deleteVocabulary(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setDeletedAt(LocalDateTime.now());
        vocabularyRepository.save(vocabulary);
    }

    private List<TopicVocabulary> buildTopicLinks(UUID vocabularyId, List<UUID> topicIds) {
        Set<UUID> uniqueTopicIds = new HashSet<>(topicIds);
        List<TopicVocabulary> links = new ArrayList<>();
        for (UUID topicId : uniqueTopicIds) {
            Topic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                    .orElseThrow(() -> new AppException(
                            HttpStatus.NOT_FOUND,
                            "TOPIC_NOT_FOUND",
                            "Topic not found"
                    ));
            if (topic.getStatus() != TopicStatus.ACTIVE) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TOPIC_INACTIVE", "Topic is inactive");
            }
            links.add(TopicVocabulary.builder()
                    .topicId(topicId)
                    .vocabularyId(vocabularyId)
                    .build());
        }
        return links;
    }

    private void syncExamples(UUID vocabularyId, List<UpdateVocabularyExampleRequest> requests) {
        List<VocabularyExample> existingExamples = vocabularyExampleRepository.findByVocabularyId(vocabularyId);
        Map<UUID, VocabularyExample> existingById = new HashMap<>();
        for (VocabularyExample example : existingExamples) {
            existingById.put(example.getId(), example);
        }

        Set<UUID> keptIds = new HashSet<>();
        Set<UUID> seenRequestIds = new HashSet<>();
        List<VocabularyExample> toSave = new ArrayList<>();

        for (UpdateVocabularyExampleRequest request : requests) {
            if (request == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_EXAMPLE", "Example is invalid");
            }
            String value = trimToNull(request.value());
            if (value == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_EXAMPLE", "Example value is required");
            }

            if (request.id() == null) {
                toSave.add(VocabularyExample.builder()
                        .vocabularyId(vocabularyId)
                        .example(value)
                        .build());
                continue;
            }

            if (!seenRequestIds.add(request.id())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "DUPLICATE_EXAMPLE_ID", "Duplicate example id");
            }

            VocabularyExample existing = existingById.get(request.id());
            if (existing == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_EXAMPLE_ID", "Example id does not belong to vocabulary");
            }

            existing.setExample(value);
            keptIds.add(existing.getId());
            toSave.add(existing);
        }

        List<VocabularyExample> toDelete = new ArrayList<>();
        for (VocabularyExample existing : existingExamples) {
            if (!keptIds.contains(existing.getId())) {
                toDelete.add(existing);
            }
        }

        if (!toDelete.isEmpty()) {
            vocabularyExampleRepository.deleteAll(toDelete);
        }
        if (!toSave.isEmpty()) {
            vocabularyExampleRepository.saveAll(toSave);
        }
    }

    private void syncTopicLinks(UUID vocabularyId, List<UUID> topicIds) {
        Set<UUID> desiredTopicIds = new LinkedHashSet<>();
        for (UUID topicId : topicIds) {
            if (topicId == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_ID", "Topic id is required");
            }
            desiredTopicIds.add(topicId);
        }

        validateActiveTopics(desiredTopicIds);

        List<TopicVocabulary> existingLinks = topicVocabularyRepository.findByVocabularyId(vocabularyId);
        Set<UUID> existingTopicIds = new HashSet<>();
        for (TopicVocabulary link : existingLinks) {
            existingTopicIds.add(link.getTopicId());
        }

        List<TopicVocabulary> toDelete = new ArrayList<>();
        for (TopicVocabulary link : existingLinks) {
            if (!desiredTopicIds.contains(link.getTopicId())) {
                toDelete.add(link);
            }
        }

        List<TopicVocabulary> toAdd = new ArrayList<>();
        for (UUID topicId : desiredTopicIds) {
            if (!existingTopicIds.contains(topicId)) {
                toAdd.add(TopicVocabulary.builder()
                        .topicId(topicId)
                        .vocabularyId(vocabularyId)
                        .build());
            }
        }

        if (!toDelete.isEmpty()) {
            topicVocabularyRepository.deleteAll(toDelete);
        }
        if (!toAdd.isEmpty()) {
            topicVocabularyRepository.saveAll(toAdd);
        }
    }

    private void importRecord(CSVRecord record) {
        String term = requireCsvField(record, "term");
        String definition = requireCsvField(record, "definition");
        String language = requireCsvField(record, "language");

        String normalizedTerm = normalizeTerm(term);
        if (normalizedTerm == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
        }
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
        }

        vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(normalizedTerm, normalizedLanguage)
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
                });

        VocabularyStatus status = parseStatus(getCsvField(record, "status"));

        Vocabulary vocabulary = Vocabulary.builder()
                .term(term.trim())
                .termNormalized(normalizedTerm)
                .definition(definition.trim())
                .definitionVi(trimToNull(getCsvField(record, "definitionVi")))
                .phonetic(trimToNull(getCsvField(record, "phonetic")))
                .partOfSpeech(trimToNull(getCsvField(record, "partOfSpeech")))
                .language(normalizedLanguage)
                .status(status)
                .build();
        vocabulary = vocabularyRepository.save(vocabulary);

        List<String> examples = parsePipedStrings(getCsvField(record, "examples"));
        if (!examples.isEmpty()) {
            List<VocabularyExample> exampleEntities = new ArrayList<>();
            for (String example : examples) {
                exampleEntities.add(VocabularyExample.builder()
                        .vocabularyId(vocabulary.getId())
                        .example(example)
                        .build());
            }
            vocabularyExampleRepository.saveAll(exampleEntities);
        }

        vocabularyAudioService.populateAudios(vocabulary);

        List<String> topicNames = parseTopicNames(record);
        if (!topicNames.isEmpty()) {
            Set<UUID> topicIdSet = resolveOrCreateTopicIds(topicNames);
            List<TopicVocabulary> links = new ArrayList<>();
            for (UUID topicId : topicIdSet) {
                links.add(TopicVocabulary.builder()
                        .topicId(topicId)
                        .vocabularyId(vocabulary.getId())
                        .build());
            }
            topicVocabularyRepository.saveAll(links);
        }
    }

    private String getCsvField(CSVRecord record, String key) {
        String headerKey = resolveHeaderKey(record, key);
        if (headerKey == null) {
            return null;
        }
        return record.get(headerKey);
    }

    private String requireCsvField(CSVRecord record, String key) {
        String value = trimToNull(getCsvField(record, key));
        if (value == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_CSV", "Missing required column: " + key);
        }
        return value;
    }

    private String resolveHeaderKey(CSVRecord record, String expectedKey) {
        if (record.isMapped(expectedKey)) {
            return expectedKey;
        }
        String normalizedExpected = normalizeCsvHeader(expectedKey);
        for (String header : record.toMap().keySet()) {
            if (normalizeCsvHeader(header).equals(normalizedExpected)) {
                return header;
            }
        }
        return null;
    }

    private String normalizeCsvHeader(String header) {
        if (header == null) {
            return "";
        }
        String normalized = header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace(" ", "");
        normalized = normalized.replace("_", "");
        normalized = normalized.replace("-", "");
        return normalized;
    }

    private VocabularyStatus parseStatus(String rawStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            return VocabularyStatus.APPROVED;
        }
        try {
            return VocabularyStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Invalid status: " + rawStatus);
        }
    }

    private List<String> parsePipedStrings(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : normalized.split("\\|")) {
            String value = trimToNull(part);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> parseTopicNames(CSVRecord record) {
        String topicsRaw = getCsvField(record, "topics");
        if (trimToNull(topicsRaw) == null) {
            topicsRaw = getCsvField(record, "topicNames");
        }
        return parsePipedStrings(topicsRaw);
    }

    private Set<UUID> resolveOrCreateTopicIds(List<String> topicNames) {
        Set<UUID> topicIds = new LinkedHashSet<>();
        for (String rawName : topicNames) {
            String topicName = trimToNull(rawName);
            if (topicName == null) {
                continue;
            }
            Topic topic = topicRepository.findByNameIgnoreCaseAndDeletedAtIsNull(topicName)
                    .orElseGet(() -> topicRepository.save(Topic.builder()
                            .name(topicName)
                            .slug(generateUniqueTopicSlug(topicName))
                            .status(TopicStatus.ACTIVE)
                            .build()));
            topicIds.add(topic.getId());
        }
        return topicIds;
    }

    private String generateUniqueTopicSlug(String name) {
        String base = normalizeSlugFromName(name);
        String slug = base;
        int suffix = 1;
        while (topicRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    private String normalizeSlugFromName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        if (normalized.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_NAME", "Topic name is invalid");
        }
        return normalized;
    }

    private void validateActiveTopics(Set<UUID> topicIds) {
        for (UUID topicId : topicIds) {
            Topic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                    .orElseThrow(() -> new AppException(
                            HttpStatus.NOT_FOUND,
                            "TOPIC_NOT_FOUND",
                            "Topic not found"
                    ));
            if (topic.getStatus() != TopicStatus.ACTIVE) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TOPIC_INACTIVE", "Topic is inactive");
            }
        }
    }

    private Page<VocabularyResponse> toResponses(Page<Vocabulary> page) {
        return toResponses(page, null);
    }

    private Page<VocabularyResponse> toResponses(Page<Vocabulary> page, UUID userId) {
        List<UUID> ids = page.stream().map(Vocabulary::getId).toList();
        Map<UUID, List<String>> examplesByVocab = loadExamples(ids);
        Map<UUID, List<VocabularyAudioResponse>> audiosByVocab = vocabularyAudioService.loadAudioResponses(ids);
        Set<UUID> myVocabIds = loadUserVocabularyIds(userId, ids);
        return page.map(vocab -> toResponse(
                vocab,
                examplesByVocab.getOrDefault(vocab.getId(), List.of()),
                audiosByVocab.getOrDefault(vocab.getId(), List.of()),
                userId == null ? null : myVocabIds.contains(vocab.getId())
        ));
    }

    private VocabularyResponse toResponse(Vocabulary vocabulary, List<String> examples, List<VocabularyAudioResponse> audios) {
        return toResponse(vocabulary, examples, audios, null);
    }

    private VocabularyResponse toResponse(
            Vocabulary vocabulary,
            List<String> examples,
            List<VocabularyAudioResponse> audios,
            Boolean inMyVocab
    ) {
        return new VocabularyResponse(
                vocabulary.getId(),
                vocabulary.getTerm(),
                vocabulary.getDefinition(),
                vocabulary.getDefinitionVi(),
                examples,
                audios,
                vocabulary.getPhonetic(),
                vocabulary.getPartOfSpeech(),
                vocabulary.getLanguage(),
                vocabulary.getStatus(),
                inMyVocab,
                vocabulary.getCreatedBy(),
                vocabulary.getCreatedAt()
        );
    }

    private VocabularyResponse withInMyVocab(VocabularyResponse response, boolean inMyVocab) {
        return new VocabularyResponse(
                response.id(),
                response.term(),
                response.definition(),
                response.definitionVi(),
                response.examples(),
                response.audios(),
                response.phonetic(),
                response.partOfSpeech(),
                response.language(),
                response.status(),
                inMyVocab,
                response.createdBy(),
                response.createdAt()
        );
    }

    private VocabularyDetailResponse toDetailResponse(
            Vocabulary vocabulary,
            List<String> examples,
            List<VocabularyAudioResponse> audios,
            List<UUID> topicIds
    ) {
        return new VocabularyDetailResponse(
                vocabulary.getId(),
                vocabulary.getTerm(),
                vocabulary.getDefinition(),
                vocabulary.getDefinitionVi(),
                examples,
                audios,
                vocabulary.getPhonetic(),
                vocabulary.getPartOfSpeech(),
                vocabulary.getLanguage(),
                vocabulary.getStatus(),
                vocabulary.getCreatedBy(),
                vocabulary.getCreatedAt(),
                topicIds
        );
    }

    private List<String> loadExamples(UUID vocabularyId) {
        return vocabularyExampleRepository.findByVocabularyId(vocabularyId).stream()
                .map(VocabularyExample::getExample)
                .toList();
    }

    private Map<UUID, List<String>> loadExamples(List<UUID> vocabularyIds) {
        Map<UUID, List<String>> result = new HashMap<>();
        if (vocabularyIds.isEmpty()) {
            return result;
        }
        List<VocabularyExample> examples = vocabularyExampleRepository.findByVocabularyIdIn(vocabularyIds);
        for (VocabularyExample example : examples) {
            result.computeIfAbsent(example.getVocabularyId(), key -> new ArrayList<>())
                    .add(example.getExample());
        }
        return result;
    }

    private Set<UUID> loadUserVocabularyIds(UUID userId, List<UUID> vocabularyIds) {
        if (userId == null || vocabularyIds == null || vocabularyIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (UserVocabulary userVocabulary : userVocabularyRepository.findByUserIdAndVocabularyIdIn(userId, vocabularyIds)) {
            ids.add(userVocabulary.getVocabularyId());
        }
        return ids;
    }

    private String normalizeTerm(String term) {
        if (term == null) {
            return null;
        }
        String trimmed = term.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return null;
        }
        String trimmed = language.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
