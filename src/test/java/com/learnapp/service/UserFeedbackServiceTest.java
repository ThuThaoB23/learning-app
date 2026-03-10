package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnapp.dto.CreateUserFeedbackRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

class UserFeedbackServiceTest {

    private final UserFeedbackRepository userFeedbackRepository = mock(UserFeedbackRepository.class);
    private final UserFeedbackAttachmentRepository userFeedbackAttachmentRepository = mock(UserFeedbackAttachmentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserFeedbackAttachmentStorageService userFeedbackAttachmentStorageService =
            mock(UserFeedbackAttachmentStorageService.class);

    private UserFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new UserFeedbackService(
                userFeedbackRepository,
                userFeedbackAttachmentRepository,
                userRepository,
                userFeedbackAttachmentStorageService
        );
    }

    @Test
    void createShouldUploadAttachmentsToMinioAndPersistOnlyMetadata() {
        UUID userId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MultipartFile file = mock(MultipartFile.class);
        CreateUserFeedbackRequest request = new CreateUserFeedbackRequest();
        request.setCategory(UserFeedbackCategory.CONTENT_ISSUE);
        request.setMessage("Definition should be clearer");
        request.setAttachments(List.of(file));

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "User")));
        when(userFeedbackRepository.save(any(UserFeedback.class))).thenAnswer(invocation -> {
            UserFeedback feedback = invocation.getArgument(0);
            feedback.setId(feedbackId);
            feedback.setCreatedAt(now);
            feedback.setUpdatedAt(now);
            return feedback;
        });
        when(userFeedbackAttachmentStorageService.uploadAttachment(feedbackId, file))
                .thenReturn(new UserFeedbackAttachmentStorageService.StoredFeedbackAttachment(
                        "feedback/" + feedbackId + "/image.png",
                        "image.png",
                        "image/png",
                        1234L,
                        "http://minio/feedback/" + feedbackId + "/image.png"
                ));
        when(userFeedbackAttachmentRepository.findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(List.of(feedbackId)))
                .thenReturn(List.of(UserFeedbackAttachment.builder()
                        .id(UUID.randomUUID())
                        .feedbackId(feedbackId)
                        .storageKey("feedback/" + feedbackId + "/image.png")
                        .fileName("image.png")
                        .contentType("image/png")
                        .fileSize(1234L)
                        .position(0)
                        .build()));
        when(userFeedbackAttachmentStorageService.buildFileUrl("feedback/" + feedbackId + "/image.png"))
                .thenReturn("http://minio/feedback/" + feedbackId + "/image.png");

        var response = service.create(userId, request);

        assertEquals(feedbackId, response.id());
        assertEquals(1, response.attachments().size());
        assertEquals("http://minio/feedback/" + feedbackId + "/image.png", response.attachments().get(0).fileUrl());

        ArgumentCaptor<List<UserFeedbackAttachment>> attachmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userFeedbackAttachmentRepository).saveAll(attachmentsCaptor.capture());
        assertEquals("feedback/" + feedbackId + "/image.png", attachmentsCaptor.getValue().get(0).getStorageKey());
    }

    @Test
    void createShouldRejectTooManyAttachments() {
        UUID userId = UUID.randomUUID();
        CreateUserFeedbackRequest request = new CreateUserFeedbackRequest();
        request.setCategory(UserFeedbackCategory.BUG_REPORT);
        request.setMessage("Too many screenshots");
        request.setAttachments(List.of(
                nonEmptyFile(),
                nonEmptyFile(),
                nonEmptyFile(),
                nonEmptyFile()
        ));

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "User")));

        AppException ex = assertThrows(AppException.class, () -> service.create(userId, request));

        assertEquals("TOO_MANY_ATTACHMENTS", ex.getErrorCode());
    }

    @Test
    void createShouldCreateGeneralFeedbackWithoutTarget() {
        UUID userId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        CreateUserFeedbackRequest request = new CreateUserFeedbackRequest();
        request.setCategory(UserFeedbackCategory.GENERAL);
        request.setMessage("Topic wording can be improved");

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "User")));
        when(userFeedbackRepository.save(any(UserFeedback.class))).thenAnswer(invocation -> {
            UserFeedback feedback = invocation.getArgument(0);
            feedback.setId(feedbackId);
            return feedback;
        });
        when(userFeedbackAttachmentRepository.findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(List.of(feedbackId)))
                .thenReturn(List.of());

        var response = service.create(userId, request);

        assertEquals("Topic wording can be improved", response.title());
        assertEquals(UserFeedbackStatus.NEW, response.status());
    }

    @Test
    void markReadShouldUpdateStatusAndReader() {
        UUID feedbackId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UserFeedback feedback = UserFeedback.builder()
                .id(feedbackId)
                .userId(UUID.randomUUID())
                .category(UserFeedbackCategory.BUG_REPORT)
                .title("Bug")
                .message("Message")
                .status(UserFeedbackStatus.NEW)
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(activeUser(adminId, "Admin")));
        when(userFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userFeedbackRepository.save(any(UserFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFeedbackAttachmentRepository.findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(List.of(feedbackId)))
                .thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(
                activeUser(feedback.getUserId(), "User"),
                activeUser(adminId, "Admin")
        ));

        var response = service.markRead(feedbackId, adminId);

        assertEquals(UserFeedbackStatus.READ, response.status());
        assertEquals(adminId, response.readBy());
    }

    @Test
    void archiveShouldSetArchivedStatusAndReadFieldsWhenMissing() {
        UUID feedbackId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UserFeedback feedback = UserFeedback.builder()
                .id(feedbackId)
                .userId(UUID.randomUUID())
                .category(UserFeedbackCategory.BUG_REPORT)
                .title("Bug")
                .message("Message")
                .status(UserFeedbackStatus.NEW)
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(activeUser(adminId, "Admin")));
        when(userFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(userFeedbackRepository.save(any(UserFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFeedbackAttachmentRepository.findByFeedbackIdInOrderByFeedbackIdAscPositionAscCreatedAtAsc(List.of(feedbackId)))
                .thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(
                activeUser(feedback.getUserId(), "User"),
                activeUser(adminId, "Admin")
        ));

        var response = service.archive(feedbackId, adminId);

        assertEquals(UserFeedbackStatus.ARCHIVED, response.status());
        assertEquals(adminId, response.readBy());
        assertEquals(adminId, response.archivedBy());
    }

    private User activeUser(UUID userId, String displayName) {
        return User.builder()
                .id(userId)
                .email(displayName.toLowerCase() + "@example.com")
                .passwordHash("hash")
                .displayName(displayName)
                .build();
    }

    private MultipartFile nonEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        return file;
    }
}
