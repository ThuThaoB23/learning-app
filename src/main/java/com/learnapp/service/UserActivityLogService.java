package com.learnapp.service;

import com.learnapp.dto.UserActivityLogResponse;
import com.learnapp.entities.TestSession;
import com.learnapp.entities.User;
import com.learnapp.entities.UserActivityLog;
import com.learnapp.entities.UserActivityTargetType;
import com.learnapp.entities.UserActivityType;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyContribution;
import com.learnapp.repository.UserActivityLogRepository;
import com.learnapp.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.learnapp.error.AppException;

@Service
@Transactional
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;

    public UserActivityLogService(
            UserActivityLogRepository userActivityLogRepository,
            UserRepository userRepository
    ) {
        this.userActivityLogRepository = userActivityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogResponse> listMyActivities(
            UUID userId,
            UserActivityType activityType,
            UserActivityTargetType targetType,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable
    ) {
        ensureUserNotDeleted(userId);
        return toResponsePage(userActivityLogRepository.search(userId, activityType, targetType, fromTime, toTime, pageable));
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogResponse> listUserActivitiesForAdmin(
            UUID userId,
            UserActivityType activityType,
            UserActivityTargetType targetType,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable
    ) {
        ensureUserExists(userId);
        return toResponsePage(userActivityLogRepository.search(userId, activityType, targetType, fromTime, toTime, pageable));
    }

    @Transactional(readOnly = true)
    public Page<UserActivityLogResponse> listActivitiesForAdmin(
            UUID userId,
            UserActivityType activityType,
            UserActivityTargetType targetType,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable
    ) {
        if (userId != null) {
            ensureUserExists(userId);
        }
        return toResponsePage(userActivityLogRepository.search(userId, activityType, targetType, fromTime, toTime, pageable));
    }

    public void logRegisterAccount(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("email", user.getEmail());
        metadata.put("displayName", user.getDisplayName());
        if (user.getRole() != null) {
            metadata.put("role", user.getRole().name());
        }

        save(
                user.getId(),
                UserActivityType.REGISTER_ACCOUNT,
                UserActivityTargetType.ACCOUNT,
                user.getId(),
                metadata
        );
    }

    public void logAddMyVocab(UUID userId, Vocabulary vocabulary, UserVocabulary userVocabulary) {
        if (userId == null || vocabulary == null || vocabulary.getId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("term", vocabulary.getTerm());
        if (vocabulary.getLanguage() != null && !vocabulary.getLanguage().isBlank()) {
            metadata.put("language", vocabulary.getLanguage());
        }
        if (userVocabulary != null && userVocabulary.getId() != null) {
            metadata.put("userVocabularyId", userVocabulary.getId().toString());
        }

        save(
                userId,
                UserActivityType.ADD_MYVOCAB,
                UserActivityTargetType.VOCABULARY,
                vocabulary.getId(),
                metadata
        );
    }

    public void logCompleteStudySession(TestSession session) {
        if (session == null || session.getId() == null || session.getUserId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (session.getType() != null) {
            metadata.put("sessionType", session.getType().name());
        }
        metadata.put("totalItems", session.getTotalItems());
        metadata.put("correctCount", session.getCorrectCount());
        metadata.put("wrongCount", session.getWrongCount());
        metadata.put("skippedCount", session.getSkippedCount());
        metadata.put("score", session.getScore());
        if (session.getScheduleDate() != null) {
            metadata.put("scheduleDate", session.getScheduleDate().toString());
        }
        if (session.getCompletedAt() != null) {
            metadata.put("completedAt", session.getCompletedAt().toString());
        }

        save(
                session.getUserId(),
                UserActivityType.COMPLETE_STUDY_SESSION,
                UserActivityTargetType.TEST_SESSION,
                session.getId(),
                metadata
        );
    }

    public void logSubmitVocabContribution(VocabularyContribution contribution, int exampleCount, int topicCount) {
        if (contribution == null || contribution.getId() == null || contribution.getContributorUserId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("term", contribution.getTerm());
        metadata.put("language", contribution.getLanguage());
        if (contribution.getPartOfSpeech() != null && !contribution.getPartOfSpeech().isBlank()) {
            metadata.put("partOfSpeech", contribution.getPartOfSpeech());
        }
        metadata.put("exampleCount", Math.max(exampleCount, 0));
        metadata.put("topicCount", Math.max(topicCount, 0));
        if (contribution.getStatus() != null) {
            metadata.put("status", contribution.getStatus().name());
        }

        save(
                contribution.getContributorUserId(),
                UserActivityType.SUBMIT_VOCAB_CONTRIBUTION,
                UserActivityTargetType.VOCABULARY_CONTRIBUTION,
                contribution.getId(),
                metadata
        );
    }

    public void logApproveVocabContribution(UUID adminUserId, VocabularyContribution contribution) {
        if (adminUserId == null || contribution == null || contribution.getId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contributionId", contribution.getId().toString());
        if (contribution.getContributorUserId() != null) {
            metadata.put("contributorUserId", contribution.getContributorUserId().toString());
        }
        metadata.put("term", contribution.getTerm());
        metadata.put("language", contribution.getLanguage());
        if (contribution.getApprovedVocabularyId() != null) {
            metadata.put("approvedVocabularyId", contribution.getApprovedVocabularyId().toString());
        }

        save(
                adminUserId,
                UserActivityType.APPROVE_VOCAB_CONTRIBUTION,
                UserActivityTargetType.VOCABULARY_CONTRIBUTION,
                contribution.getId(),
                metadata
        );
    }

    public void logRejectVocabContribution(UUID adminUserId, VocabularyContribution contribution) {
        if (adminUserId == null || contribution == null || contribution.getId() == null) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contributionId", contribution.getId().toString());
        if (contribution.getContributorUserId() != null) {
            metadata.put("contributorUserId", contribution.getContributorUserId().toString());
        }
        metadata.put("term", contribution.getTerm());
        metadata.put("language", contribution.getLanguage());
        if (contribution.getRejectReason() != null) {
            metadata.put("rejectReason", contribution.getRejectReason().name());
        }

        save(
                adminUserId,
                UserActivityType.REJECT_VOCAB_CONTRIBUTION,
                UserActivityTargetType.VOCABULARY_CONTRIBUTION,
                contribution.getId(),
                metadata
        );
    }

    private void save(
            UUID userId,
            UserActivityType activityType,
            UserActivityTargetType targetType,
            UUID targetId,
            Map<String, Object> metadata
    ) {
        userActivityLogRepository.save(UserActivityLog.builder()
                .userId(userId)
                .activityType(activityType)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata == null || metadata.isEmpty() ? null : metadata)
                .build());
    }

    private Page<UserActivityLogResponse> toResponsePage(Page<UserActivityLog> page) {
        Map<UUID, String> userNames = loadUserDisplayNames(page.getContent());
        return page.map(log -> toResponse(log, userNames.get(log.getUserId())));
    }

    private Map<UUID, String> loadUserDisplayNames(List<UserActivityLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Map.of();
        }

        List<UUID> userIds = logs.stream()
                .map(UserActivityLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> namesByUserId = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> {
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = user.getUsername();
            }
            if ((displayName == null || displayName.isBlank()) && user.getEmail() != null) {
                displayName = user.getEmail();
            }
            namesByUserId.put(user.getId(), displayName);
        });
        return namesByUserId;
    }

    private UserActivityLogResponse toResponse(UserActivityLog log, String userDisplayName) {
        return new UserActivityLogResponse(
                log.getId(),
                log.getUserId(),
                userDisplayName,
                log.getActivityType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }

    private void ensureUserExists(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private void ensureUserNotDeleted(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
    }
}
