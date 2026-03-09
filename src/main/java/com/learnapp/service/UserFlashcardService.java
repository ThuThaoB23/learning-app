package com.learnapp.service;

import com.learnapp.dto.FlashcardDeckBucket;
import com.learnapp.dto.FlashcardDeckGroupResponse;
import com.learnapp.dto.FlashcardDeckResponse;
import com.learnapp.dto.FlashcardItemResponse;
import com.learnapp.dto.VocabularyAudioResponse;
import com.learnapp.entities.User;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyExample;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyExampleRepository;
import com.learnapp.repository.VocabularyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserFlashcardService {

    public static final int DEFAULT_LIMIT = 20;

    private final UserRepository userRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final VocabularyExampleRepository vocabularyExampleRepository;
    private final VocabularyAudioService vocabularyAudioService;
    private final FlashcardSelectionService flashcardSelectionService;

    public UserFlashcardService(
            UserRepository userRepository,
            UserVocabularyRepository userVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            VocabularyExampleRepository vocabularyExampleRepository,
            VocabularyAudioService vocabularyAudioService,
            FlashcardSelectionService flashcardSelectionService
    ) {
        this.userRepository = userRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyExampleRepository = vocabularyExampleRepository;
        this.vocabularyAudioService = vocabularyAudioService;
        this.flashcardSelectionService = flashcardSelectionService;
    }

    public FlashcardDeckResponse buildDeck(UUID userId, int limit) {
        User user = ensureUserNotDeleted(userId);
        ZoneId zoneId = resolveZone(user.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);

        List<FlashcardSelectionService.SelectedFlashcard> selected = flashcardSelectionService.select(
                userVocabularyRepository.findByUserId(userId),
                limit,
                today,
                zoneId
        );
        if (selected.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_USER_VOCAB", "No vocabulary found in user list");
        }

        List<UUID> vocabularyIds = selected.stream()
                .map(item -> item.userVocabulary().getVocabularyId())
                .distinct()
                .toList();
        Map<UUID, Vocabulary> vocabulariesById = loadVocabularies(vocabularyIds);
        if (vocabulariesById.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_ELIGIBLE_VOCAB", "No approved vocabulary available");
        }

        Map<UUID, List<String>> examplesByVocabularyId = loadExamples(vocabularyIds);
        Map<UUID, List<VocabularyAudioResponse>> audiosByVocabularyId = vocabularyAudioService.loadAudioResponses(vocabularyIds);

        List<FlashcardItemResponse> items = new ArrayList<>();
        for (FlashcardSelectionService.SelectedFlashcard selectedItem : selected) {
            UUID vocabularyId = selectedItem.userVocabulary().getVocabularyId();
            Vocabulary vocabulary = vocabulariesById.get(vocabularyId);
            if (vocabulary == null) {
                continue;
            }
            items.add(new FlashcardItemResponse(
                    selectedItem.userVocabulary().getId(),
                    vocabularyId,
                    selectedItem.bucket(),
                    vocabulary.getTerm(),
                    vocabulary.getDefinition(),
                    vocabulary.getDefinitionVi(),
                    examplesByVocabularyId.getOrDefault(vocabularyId, List.of()),
                    audiosByVocabularyId.getOrDefault(vocabularyId, List.of()),
                    vocabulary.getPhonetic(),
                    vocabulary.getPartOfSpeech(),
                    vocabulary.getLanguage(),
                    selectedItem.userVocabulary().getStatus(),
                    selectedItem.userVocabulary().getProcess(),
                    selectedItem.userVocabulary().getLastReviewedAt(),
                    selectedItem.userVocabulary().getNextDueAt()
            ));
        }

        if (items.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_ELIGIBLE_VOCAB", "No approved vocabulary available");
        }

        return new FlashcardDeckResponse(
                limit,
                items.size(),
                buildGroups(items),
                items
        );
    }

    private Map<UUID, Vocabulary> loadVocabularies(List<UUID> vocabularyIds) {
        Map<UUID, Vocabulary> vocabulariesById = new HashMap<>();
        vocabularyRepository.findByIdInAndStatusAndDeletedAtIsNull(vocabularyIds, VocabularyStatus.APPROVED)
                .forEach(vocabulary -> vocabulariesById.put(vocabulary.getId(), vocabulary));
        return vocabulariesById;
    }

    private Map<UUID, List<String>> loadExamples(List<UUID> vocabularyIds) {
        Map<UUID, List<String>> examplesByVocabularyId = new HashMap<>();
        for (VocabularyExample example : vocabularyExampleRepository.findByVocabularyIdIn(vocabularyIds)) {
            examplesByVocabularyId
                    .computeIfAbsent(example.getVocabularyId(), ignored -> new ArrayList<>())
                    .add(example.getExample());
        }
        return examplesByVocabularyId;
    }

    private List<FlashcardDeckGroupResponse> buildGroups(List<FlashcardItemResponse> items) {
        Map<FlashcardDeckBucket, Integer> counts = new EnumMap<>(FlashcardDeckBucket.class);
        for (FlashcardItemResponse item : items) {
            counts.merge(item.bucket(), 1, Integer::sum);
        }

        List<FlashcardDeckGroupResponse> groups = new ArrayList<>();
        for (FlashcardDeckBucket bucket : FlashcardDeckBucket.values()) {
            Integer count = counts.get(bucket);
            if (count != null && count > 0) {
                groups.add(new FlashcardDeckGroupResponse(bucket, count));
            }
        }
        return groups;
    }

    private User ensureUserNotDeleted(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        return user;
    }

    private ZoneId resolveZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timeZone);
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }
}
