package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnapp.dto.FlashcardDeckBucket;
import com.learnapp.entities.UserVocabulary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlashcardSelectionServiceTest {

    private FlashcardSelectionService service;

    @BeforeEach
    void setUp() {
        service = new FlashcardSelectionService(new SpacedRepetitionService());
    }

    @Test
    void selectShouldPrioritizeDueWeakAndNewBuckets() {
        LocalDate today = LocalDate.of(2026, 3, 9);
        ZoneId zoneId = ZoneId.of("UTC");

        List<UserVocabulary> items = List.of(
                userVocabulary(10, LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 3, 5, 8, 0), 1),
                userVocabulary(20, LocalDateTime.of(2026, 3, 2, 8, 0), LocalDateTime.of(2026, 3, 4, 8, 0), 2),
                userVocabulary(30, LocalDateTime.of(2026, 3, 7, 8, 0), LocalDateTime.of(2026, 3, 12, 8, 0), 3),
                userVocabulary(40, LocalDateTime.of(2026, 3, 6, 8, 0), LocalDateTime.of(2026, 3, 13, 8, 0), 4),
                userVocabulary(0, null, null, 5),
                userVocabulary(0, null, null, 6),
                userVocabulary(85, LocalDateTime.of(2026, 3, 8, 8, 0), LocalDateTime.of(2026, 3, 20, 8, 0), 7)
        );

        List<FlashcardSelectionService.SelectedFlashcard> selected = service.select(items, 5, today, zoneId);

        assertEquals(5, selected.size());
        assertEquals(FlashcardDeckBucket.DUE, selected.get(0).bucket());
        assertEquals(FlashcardDeckBucket.DUE, selected.get(1).bucket());
        assertEquals(FlashcardDeckBucket.WEAK, selected.get(2).bucket());
        assertEquals(FlashcardDeckBucket.NEW, selected.get(3).bucket());
        assertEquals(FlashcardDeckBucket.NEW, selected.get(4).bucket());
    }

    @Test
    void selectShouldFallbackToRemainingItemsWhenPrimaryBucketsAreInsufficient() {
        LocalDate today = LocalDate.of(2026, 3, 9);
        ZoneId zoneId = ZoneId.of("UTC");

        List<UserVocabulary> items = List.of(
                userVocabulary(95, LocalDateTime.of(2026, 3, 8, 8, 0), LocalDateTime.of(2026, 3, 25, 8, 0), 1),
                userVocabulary(90, LocalDateTime.of(2026, 3, 7, 8, 0), LocalDateTime.of(2026, 3, 22, 8, 0), 2),
                userVocabulary(0, null, null, 3)
        );

        List<FlashcardSelectionService.SelectedFlashcard> selected = service.select(items, 3, today, zoneId);

        assertEquals(3, selected.size());
        assertEquals(FlashcardDeckBucket.NEW, selected.get(0).bucket());
        assertTrue(selected.stream().anyMatch(item -> item.bucket() == FlashcardDeckBucket.REVIEW));
    }

    @Test
    void selectShouldAvoidRecentlyServedItemsWhenAlternativesExist() {
        LocalDate today = LocalDate.of(2026, 3, 9);
        ZoneId zoneId = ZoneId.of("UTC");

        UserVocabulary dueOne = userVocabulary(10, LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 3, 5, 8, 0), 1);
        UserVocabulary dueTwo = userVocabulary(20, LocalDateTime.of(2026, 3, 2, 8, 0), LocalDateTime.of(2026, 3, 4, 8, 0), 2);
        UserVocabulary dueThree = userVocabulary(30, LocalDateTime.of(2026, 3, 3, 8, 0), LocalDateTime.of(2026, 3, 3, 8, 0), 3);

        List<FlashcardSelectionService.SelectedFlashcard> selected = service.select(
                List.of(dueOne, dueTwo, dueThree),
                2,
                today,
                zoneId,
                Set.of(dueOne.getId(), dueTwo.getId())
        );

        assertEquals(2, selected.size());
        assertEquals(dueThree.getId(), selected.get(0).userVocabulary().getId());
    }

    private UserVocabulary userVocabulary(
            int process,
            LocalDateTime lastReviewedAt,
            LocalDateTime nextDueAt,
            int sequence
    ) {
        return UserVocabulary.builder()
                .id(UUID.nameUUIDFromBytes(("uv-" + sequence).getBytes()))
                .vocabularyId(UUID.nameUUIDFromBytes(("vocab-" + sequence).getBytes()))
                .process(process)
                .lastReviewedAt(lastReviewedAt)
                .nextDueAt(nextDueAt)
                .createdAt(LocalDateTime.of(2026, 3, sequence, 9, 0))
                .build();
    }
}
