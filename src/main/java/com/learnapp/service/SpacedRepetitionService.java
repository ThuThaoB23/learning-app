package com.learnapp.service;

import com.learnapp.entities.UserVocabulary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class SpacedRepetitionService {

    public int intervalDays(int process) {
        int bounded = clampProcess(process);
        if (bounded <= 20) {
            return 1;
        }
        if (bounded <= 40) {
            return 2;
        }
        if (bounded <= 60) {
            return 4;
        }
        if (bounded <= 75) {
            return 7;
        }
        if (bounded <= 90) {
            return 14;
        }
        return 30;
    }

    public int reward(int process) {
        return Math.max(2, 10 - Math.floorDiv(clampProcess(process), 15));
    }

    public int penalty(int process) {
        return Math.max(6, 12 - Math.floorDiv(clampProcess(process), 20));
    }

    public void applyAttempt(UserVocabulary userVocabulary, boolean isCorrect, LocalDateTime now) {
        int process = nullSafe(userVocabulary.getProcess());

        if (isCorrect) {
            process = clampProcess(process + reward(process));
            userVocabulary.setStreak(nullSafe(userVocabulary.getStreak()) + 1);
            userVocabulary.setRightCount(nullSafe(userVocabulary.getRightCount()) + 1);
        } else {
            process = clampProcess(process - penalty(process));
            userVocabulary.setStreak(0);
            userVocabulary.setWrongCount(nullSafe(userVocabulary.getWrongCount()) + 1);
        }

        userVocabulary.setProcess(process);
        userVocabulary.setLastReviewedAt(now);
        userVocabulary.setNextDueAt(now.plusDays(intervalDays(process)));
    }

    public boolean isDue(UserVocabulary userVocabulary, LocalDate today, ZoneId zoneId) {
        if (userVocabulary.getNextDueAt() == null) {
            return true;
        }
        LocalDate dueDate = userVocabulary.getNextDueAt().atZone(zoneId).toLocalDate();
        return !dueDate.isAfter(today);
    }

    public long overdueDays(UserVocabulary userVocabulary, LocalDate today, ZoneId zoneId) {
        if (userVocabulary.getNextDueAt() == null) {
            return 0;
        }
        LocalDate dueDate = userVocabulary.getNextDueAt().atZone(zoneId).toLocalDate();
        if (!dueDate.isBefore(today)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, today);
    }

    public double priorityScore(UserVocabulary userVocabulary, LocalDate today, ZoneId zoneId) {
        long overdue = overdueDays(userVocabulary, today, zoneId);
        long sinceLast = daysSinceLast(userVocabulary, today, zoneId);
        int weakness = 100 - clampProcess(nullSafe(userVocabulary.getProcess()));
        return 3.0 * overdue + 0.05 * weakness + 0.1 * sinceLast;
    }

    private long daysSinceLast(UserVocabulary userVocabulary, LocalDate today, ZoneId zoneId) {
        if (userVocabulary.getLastReviewedAt() == null) {
            return 30;
        }
        LocalDate lastDate = userVocabulary.getLastReviewedAt().atZone(zoneId).toLocalDate();
        return Math.max(0, ChronoUnit.DAYS.between(lastDate, today));
    }

    private int clampProcess(int process) {
        return Math.min(100, Math.max(0, process));
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
