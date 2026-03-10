package com.learnapp.service;

import com.learnapp.dto.AdminUserFeedbackDetailResponse;
import com.learnapp.dto.AdminUserFeedbackQueueItemResponse;
import com.learnapp.dto.CreateUserFeedbackRequest;
import com.learnapp.dto.UserFeedbackAttachmentResponse;
import com.learnapp.dto.UserFeedbackResponse;
import com.learnapp.entities.User;
import com.learnapp.entities.UserFeedback;
import com.learnapp.entities.UserFeedbackAttachment;
import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserFeedbackAttachmentRepository;
import com.learnapp.repository.UserFeedbackRepository;
import com.learnapp.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UserFeedbackService {

    private static final int MAX_ATTACHMENTS = 3;

    private final UserFeedbackRepository userFeedbackRepository;
    private final UserFeedbackAttachmentRepository userFeedbackAttachmentRepository;
    private final UserRepository userRepository;
    private final UserFeedbackAttachmentStorageService userFeedbackAttachmentStorageService;

    public UserFeedbackService(
            UserFeedbackRepository userFeedbackRepository,
            UserFeedbackAttachmentRepository userFeedbackAttachmentRepository,
            UserRepository userRepository,
            UserFeedbackAttachmentStorageService userFeedbackAttachmentStorageService
    ) {
        this.userFeedbackRepository = userFeedbackRepository;
        this.userFeedbackAttachmentRepository = userFeedbackAttachmentRepository;
        this.userRepository = userRepository;
        this.userFeedbackAttachmentStorageService = userFeedbackAttachmentStorageService;
    }

    public UserFeedbackResponse create(UUID userId, CreateUserFeedbackRequest request) {
        ensureUserNotDeleted(userId);

        String message = requireMessage(request.getMessage());
        List<MultipartFile> attachments = sanitizeAttachments(request.getAttachments());
        validateAttachmentCount(attachments);

        UserFeedback feedback = userFeedbackRepository.save(UserFeedback.builder()
                .userId(userId)
                .category(request.getCategory())
                .title(resolveTitle(request.getTitle(), message))
                .message(message)
                .status(UserFeedbackStatus.NEW)
                .sourceScreen(trimToNull(request.getSourceScreen()))
                .appVersion(trimToNull(request.getAppVersion()))
                .deviceInfo(trimToNull(request.getDeviceInfo()))
                .locale(trimToNull(request.getLocale()))
                .build());

        List<String> uploadedStorageKeys = new ArrayList<>();
        try {
            saveAttachments(feedback.getId(), attachments, uploadedStorageKeys);
            return toUserResponse(feedback, attachmentMap(List.of(feedback.getId())));
        } catch (RuntimeException ex) {
            cleanupUploadedAttachments(uploadedStorageKeys);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<UserFeedbackResponse> listMine(UUID userId, Pageable pageable) {
        ensureUserNotDeleted(userId);
        Page<UserFeedback> page = userFeedbackRepository.findByUserId(userId, pageable);
        Map<UUID, List<UserFeedbackAttachmentResponse>> attachmentsByFeedbackId = attachmentMap(
                page.stream().map(UserFeedback::getId).toList()
        );
        return page.map(feedback -> toUserResponse(feedback, attachmentsByFeedbackId));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserFeedbackQueueItemResponse> searchForAdmin(
            String query,
            UserFeedbackStatus status,
            UserFeedbackCategory category,
            Pageable pageable
    ) {
        Page<UserFeedback> page = userFeedbackRepository.searchForAdmin(
                normalizeQuery(query),
                status,
                category,
                pageable
        );
        Map<UUID, String> userNames = loadUserDisplayNames(page.stream().map(UserFeedback::getUserId).toList());
        Map<UUID, Long> attachmentCounts = countAttachments(page.stream().map(UserFeedback::getId).toList());
        return page.map(feedback -> new AdminUserFeedbackQueueItemResponse(
                feedback.getId(),
                feedback.getUserId(),
                userNames.get(feedback.getUserId()),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getStatus(),
                attachmentCounts.getOrDefault(feedback.getId(), 0L),
                feedback.getCreatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public AdminUserFeedbackDetailResponse getDetailForAdmin(UUID feedbackId) {
        UserFeedback feedback = findFeedback(feedbackId);
        return toAdminDetailResponse(feedback, attachmentMap(List.of(feedbackId)));
    }

    public AdminUserFeedbackDetailResponse markRead(UUID feedbackId, UUID adminUserId) {
        ensureUserExists(adminUserId);
        UserFeedback feedback = findFeedback(feedbackId);
        if (feedback.getStatus() == UserFeedbackStatus.NEW) {
            feedback.setStatus(UserFeedbackStatus.READ);
            feedback.setReadBy(adminUserId);
            feedback.setReadAt(LocalDateTime.now());
            feedback = userFeedbackRepository.save(feedback);
        } else if (feedback.getStatus() == UserFeedbackStatus.READ && feedback.getReadAt() == null) {
            feedback.setReadBy(adminUserId);
            feedback.setReadAt(LocalDateTime.now());
            feedback = userFeedbackRepository.save(feedback);
        }
        return toAdminDetailResponse(feedback, attachmentMap(List.of(feedbackId)));
    }

    public AdminUserFeedbackDetailResponse archive(UUID feedbackId, UUID adminUserId) {
        ensureUserExists(adminUserId);
        UserFeedback feedback = findFeedback(feedbackId);
        if (feedback.getStatus() != UserFeedbackStatus.ARCHIVED) {
            LocalDateTime now = LocalDateTime.now();
            if (feedback.getReadAt() == null) {
                feedback.setReadBy(adminUserId);
                feedback.setReadAt(now);
            }
            feedback.setStatus(UserFeedbackStatus.ARCHIVED);
            feedback.setArchivedBy(adminUserId);
            feedback.setArchivedAt(now);
            feedback = userFeedbackRepository.save(feedback);
        }
        return toAdminDetailResponse(feedback, attachmentMap(List.of(feedbackId)));
    }

    private void saveAttachments(UUID feedbackId, List<MultipartFile> attachments, List<String> uploadedStorageKeys) {
        if (attachments.isEmpty()) {
            return;
        }
        List<UserFeedbackAttachment> records = new ArrayList<>();
        for (int i = 0; i < attachments.size(); i++) {
            MultipartFile file = attachments.get(i);
            UserFeedbackAttachmentStorageService.StoredFeedbackAttachment stored =
                    userFeedbackAttachmentStorageService.uploadAttachment(feedbackId, file);
            uploadedStorageKeys.add(stored.storageKey());
            records.add(UserFeedbackAttachment.builder()
                    .feedbackId(feedbackId)
                    .storageKey(stored.storageKey())
                    .fileName(stored.fileName())
                    .contentType(stored.contentType())
                    .fileSize(stored.fileSize())
                    .position(i)
                    .build());
        }
        userFeedbackAttachmentRepository.saveAll(records);
    }

    private void cleanupUploadedAttachments(List<String> uploadedStorageKeys) {
        for (String storageKey : uploadedStorageKeys) {
            userFeedbackAttachmentStorageService.deleteByStorageKey(storageKey);
        }
    }

    private UserFeedback findFeedback(UUID feedbackId) {
        return userFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "FEEDBACK_NOT_FOUND", "Feedback not found"));
    }

    private String requireMessage(String message) {
        String normalized = trimToNull(message);
        if (normalized == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_MESSAGE", "Feedback message is required");
        }
        return normalized;
    }

    private void validateAttachmentCount(List<MultipartFile> attachments) {
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "TOO_MANY_ATTACHMENTS",
                    "At most 3 feedback attachments are allowed"
            );
        }
    }

    private List<MultipartFile> sanitizeAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private String resolveTitle(String requestedTitle, String message) {
        String normalized = trimToNull(requestedTitle);
        if (normalized != null) {
            return normalized;
        }
        if (message.length() <= 120) {
            return message;
        }
        return message.substring(0, 117) + "...";
    }

    private Map<UUID, List<UserFeedbackAttachmentResponse>> attachmentMap(Collection<UUID> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UserFeedbackAttachmentResponse>> attachmentsByFeedbackId = new LinkedHashMap<>();
        for (UserFeedbackAttachment attachment : userFeedbackAttachmentRepository
                .findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(feedbackIds)) {
            attachmentsByFeedbackId.computeIfAbsent(attachment.getFeedbackId(), ignored -> new ArrayList<>())
                    .add(toAttachmentResponse(attachment));
        }
        return attachmentsByFeedbackId;
    }

    private Map<UUID, Long> countAttachments(Collection<UUID> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (UserFeedbackAttachment attachment : userFeedbackAttachmentRepository
                .findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(feedbackIds)) {
            counts.merge(attachment.getFeedbackId(), 1L, Long::sum);
        }
        return counts;
    }

    private UserFeedbackResponse toUserResponse(
            UserFeedback feedback,
            Map<UUID, List<UserFeedbackAttachmentResponse>> attachmentsByFeedbackId
    ) {
        return new UserFeedbackResponse(
                feedback.getId(),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getMessage(),
                feedback.getStatus(),
                feedback.getSourceScreen(),
                feedback.getAppVersion(),
                feedback.getDeviceInfo(),
                feedback.getLocale(),
                attachmentsByFeedbackId.getOrDefault(feedback.getId(), List.of()),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }

    private AdminUserFeedbackDetailResponse toAdminDetailResponse(
            UserFeedback feedback,
            Map<UUID, List<UserFeedbackAttachmentResponse>> attachmentsByFeedbackId
    ) {
        Map<UUID, String> userNames = loadUserDisplayNames(java.util.stream.Stream.of(
                        feedback.getUserId(),
                        feedback.getReadBy(),
                        feedback.getArchivedBy()
                )
                .filter(Objects::nonNull)
                .toList());
        return new AdminUserFeedbackDetailResponse(
                feedback.getId(),
                feedback.getUserId(),
                userNames.get(feedback.getUserId()),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getMessage(),
                feedback.getStatus(),
                feedback.getSourceScreen(),
                feedback.getAppVersion(),
                feedback.getDeviceInfo(),
                feedback.getLocale(),
                feedback.getReadBy(),
                userNames.get(feedback.getReadBy()),
                feedback.getReadAt(),
                feedback.getArchivedBy(),
                userNames.get(feedback.getArchivedBy()),
                feedback.getArchivedAt(),
                attachmentsByFeedbackId.getOrDefault(feedback.getId(), List.of()),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }

    private UserFeedbackAttachmentResponse toAttachmentResponse(UserFeedbackAttachment attachment) {
        return new UserFeedbackAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getPosition(),
                userFeedbackAttachmentStorageService.buildFileUrl(attachment.getStorageKey())
        );
    }

    private Map<UUID, String> loadUserDisplayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> result = new HashMap<>();
        List<UUID> filteredIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (filteredIds.isEmpty()) {
            return result;
        }
        userRepository.findAllById(filteredIds)
                .forEach(user -> result.put(user.getId(), user.getDisplayName()));
        return result;
    }

    private String normalizeQuery(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void ensureUserNotDeleted(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
    }

    private void ensureUserExists(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }
}
