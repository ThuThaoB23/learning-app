package com.learnapp.service;

import com.learnapp.dto.FlashcardDeckBucket;
import com.learnapp.entities.UserVocabulary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FlashcardSelectionService {

    private static final int WEAK_THRESHOLD = 50;

    private final SpacedRepetitionService spacedRepetitionService;

    public FlashcardSelectionService(SpacedRepetitionService spacedRepetitionService) {
        this.spacedRepetitionService = spacedRepetitionService;
    }

    public List<SelectedFlashcard> select(
            List<UserVocabulary> allItems,
            int limit,
            LocalDate today,
            ZoneId zoneId
    ) {
        return select(allItems, limit, today, zoneId, Set.of());
    }

    public List<SelectedFlashcard> select(
            List<UserVocabulary> allItems,
            int limit,
            LocalDate today,
            ZoneId zoneId,
            Set<UUID> recentlyServedIds
    ) {
        if (allItems == null || allItems.isEmpty() || limit <= 0) {
            return List.of();
        }

        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        List<SelectedFlashcard> rankedItems = allItems.stream()
                .map(item -> new SelectedFlashcard(
                        item,
                        classifyBucket(item, endOfDay),
                        spacedRepetitionService.priorityScore(item, today, zoneId)
                ))
                .toList();

        List<SelectedFlashcard> dueItems = rankedItems.stream()
                .filter(item -> item.bucket() == FlashcardDeckBucket.DUE)
                .sorted(byPriority(recentlyServedIds))
                .toList();
        List<SelectedFlashcard> weakItems = rankedItems.stream()
                .filter(item -> item.bucket() == FlashcardDeckBucket.WEAK)
                .sorted(byPriority(recentlyServedIds))
                .toList();
        List<SelectedFlashcard> newItems = rankedItems.stream()
                .filter(item -> item.bucket() == FlashcardDeckBucket.NEW)
                .sorted(byNewness(recentlyServedIds))
                .toList();

        int dueQuota = calculateDueQuota(limit);
        int weakQuota = calculateWeakQuota(limit, dueQuota);
        int newQuota = Math.max(0, limit - dueQuota - weakQuota);

        List<SelectedFlashcard> selected = new ArrayList<>();
        Set<UUID> pickedIds = new HashSet<>();

        pick(dueItems, dueQuota, selected, pickedIds);
        pick(weakItems, weakQuota, selected, pickedIds);
        pick(newItems, newQuota, selected, pickedIds);

        if (selected.size() < limit) {
            List<SelectedFlashcard> remaining = rankedItems.stream()
                    .filter(item -> !pickedIds.contains(item.userVocabulary().getId()))
                    .sorted(byPriority(recentlyServedIds))
                    .toList();
            pick(remaining, limit - selected.size(), selected, pickedIds);
        }

        return selected;
    }

    private FlashcardDeckBucket classifyBucket(UserVocabulary userVocabulary, LocalDateTime endOfDay) {
        if (userVocabulary.getLastReviewedAt() == null || userVocabulary.getNextDueAt() == null) {
            return FlashcardDeckBucket.NEW;
        }
        if (!userVocabulary.getNextDueAt().isAfter(endOfDay)) {
            return FlashcardDeckBucket.DUE;
        }
        if (nullSafe(userVocabulary.getProcess()) <= WEAK_THRESHOLD) {
            return FlashcardDeckBucket.WEAK;
        }
        return FlashcardDeckBucket.REVIEW;
    }

    private Comparator<SelectedFlashcard> byPriority(Set<UUID> recentlyServedIds) {
        return Comparator.comparing((SelectedFlashcard item) -> isRecentlyServed(item, recentlyServedIds))
                .thenComparing(SelectedFlashcard::priorityScore, Comparator.reverseOrder())
                .thenComparing(item -> item.userVocabulary().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<SelectedFlashcard> byNewness(Set<UUID> recentlyServedIds) {
        return Comparator.comparing((SelectedFlashcard item) -> isRecentlyServed(item, recentlyServedIds))
                .thenComparing(
                item -> item.userVocabulary().getCreatedAt(),
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    private int calculateDueQuota(int limit) {
        return Math.max(1, (int) Math.floor(limit * 0.7d));
    }

    private int calculateWeakQuota(int limit, int dueQuota) {
        if (limit <= 4) {
            return 0;
        }
        int weakQuota = Math.max(1, (int) Math.floor(limit * 0.2d));
        return Math.min(weakQuota, Math.max(0, limit - dueQuota));
    }

    private void pick(
            List<SelectedFlashcard> source,
            int limit,
            List<SelectedFlashcard> target,
            Set<UUID> pickedIds
    ) {
        int added = 0;
        for (SelectedFlashcard item : source) {
            if (added >= limit) {
                return;
            }
            UUID userVocabularyId = item.userVocabulary().getId();
            if (userVocabularyId != null && pickedIds.add(userVocabularyId)) {
                target.add(item);
                added++;
            }
        }
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isRecentlyServed(SelectedFlashcard item, Set<UUID> recentlyServedIds) {
        UUID userVocabularyId = item.userVocabulary().getId();
        return userVocabularyId != null && recentlyServedIds.contains(userVocabularyId);
    }

    public record SelectedFlashcard(
            UserVocabulary userVocabulary,
            FlashcardDeckBucket bucket,
            double priorityScore
    ) {}
}
