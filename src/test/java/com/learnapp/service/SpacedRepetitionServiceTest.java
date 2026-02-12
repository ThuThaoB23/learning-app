package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnapp.entities.UserVocabulary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpacedRepetitionServiceTest {

    private SpacedRepetitionService service;

    @BeforeEach
    void setUp() {
        service = new SpacedRepetitionService();
    }

    @Test
    void intervalDaysShouldFollowConfiguredBuckets() {
        assertEquals(1, service.intervalDays(0));
        assertEquals(2, service.intervalDays(21));
        assertEquals(4, service.intervalDays(41));
        assertEquals(7, service.intervalDays(61));
        assertEquals(14, service.intervalDays(76));
        assertEquals(30, service.intervalDays(95));
    }

    @Test
    void applyAttemptShouldIncreaseProcessForCorrectAnswer() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 11, 10, 0);
        UserVocabulary uv = UserVocabulary.builder()
                .process(30)
                .streak(0)
                .rightCount(2)
                .wrongCount(1)
                .build();

        service.applyAttempt(uv, true, now);

        assertEquals(38, uv.getProcess());
        assertEquals(1, uv.getStreak());
        assertEquals(3, uv.getRightCount());
        assertEquals(1, uv.getWrongCount());
        assertEquals(now, uv.getLastReviewedAt());
        assertEquals(now.plusDays(2), uv.getNextDueAt());
    }

    @Test
    void applyAttemptShouldDecreaseProcessForWrongAnswer() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 11, 10, 0);
        UserVocabulary uv = UserVocabulary.builder()
                .process(80)
                .streak(5)
                .rightCount(8)
                .wrongCount(2)
                .build();

        service.applyAttempt(uv, false, now);

        assertEquals(72, uv.getProcess());
        assertEquals(0, uv.getStreak());
        assertEquals(8, uv.getRightCount());
        assertEquals(3, uv.getWrongCount());
        assertEquals(now.plusDays(7), uv.getNextDueAt());
    }

    @Test
    void isDueShouldReturnTrueForNullDueDateOrPastDate() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.of(2026, 2, 11);

        UserVocabulary newItem = UserVocabulary.builder().build();
        assertTrue(service.isDue(newItem, today, zoneId));

        UserVocabulary overdue = UserVocabulary.builder()
                .nextDueAt(LocalDateTime.of(2026, 2, 10, 9, 0))
                .build();
        assertTrue(service.isDue(overdue, today, zoneId));

        UserVocabulary future = UserVocabulary.builder()
                .nextDueAt(LocalDateTime.of(2026, 2, 12, 9, 0))
                .build();
        assertFalse(service.isDue(future, today, zoneId));
    }

    @Test
    void priorityScoreShouldPreferOverdueAndWeakItems() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.of(2026, 2, 11);

        UserVocabulary weakAndOverdue = UserVocabulary.builder()
                .process(20)
                .lastReviewedAt(LocalDateTime.of(2026, 2, 5, 8, 0))
                .nextDueAt(LocalDateTime.of(2026, 2, 7, 8, 0))
                .build();

        UserVocabulary strongAndNotDue = UserVocabulary.builder()
                .process(90)
                .lastReviewedAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .nextDueAt(LocalDateTime.of(2026, 2, 15, 8, 0))
                .build();

        double weakScore = service.priorityScore(weakAndOverdue, today, zoneId);
        double strongScore = service.priorityScore(strongAndNotDue, today, zoneId);

        assertTrue(weakScore > strongScore);
    }
}
