package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learnapp.dto.SubmitTestSessionAnswersRequest;
import com.learnapp.entities.QuestionType;
import com.learnapp.entities.TestItem;
import com.learnapp.entities.TestItemStatus;
import com.learnapp.entities.TestSession;
import com.learnapp.entities.TestSessionSourceType;
import com.learnapp.entities.TestSessionStatus;
import com.learnapp.entities.TestSessionType;
import com.learnapp.entities.User;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.error.AppException;
import com.learnapp.repository.TestItemRepository;
import com.learnapp.repository.TestSessionRepository;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestSessionServiceTest {

    private final TestSessionRepository testSessionRepository = mock(TestSessionRepository.class);
    private final TestItemRepository testItemRepository = mock(TestItemRepository.class);
    private final UserVocabularyRepository userVocabularyRepository = mock(UserVocabularyRepository.class);
    private final VocabularyRepository vocabularyRepository = mock(VocabularyRepository.class);
    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final TopicVocabularyRepository topicVocabularyRepository = mock(TopicVocabularyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SpacedRepetitionService spacedRepetitionService = mock(SpacedRepetitionService.class);
    private final QuestionEngineService questionEngineService = mock(QuestionEngineService.class);
    private final VocabularyAudioService vocabularyAudioService = mock(VocabularyAudioService.class);
    private final UserActivityLogService userActivityLogService = mock(UserActivityLogService.class);

    private TestSessionService service;

    @BeforeEach
    void setUp() {
        service = new TestSessionService(
                testSessionRepository,
                testItemRepository,
                userVocabularyRepository,
                vocabularyRepository,
                topicRepository,
                topicVocabularyRepository,
                userRepository,
                spacedRepetitionService,
                questionEngineService,
                vocabularyAudioService,
                userActivityLogService
        );
    }

    @Test
    void submitAllAnswersShouldRejectWhenAnyPendingItemIsMissingFromPayload() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID firstItemId = UUID.randomUUID();
        UUID secondItemId = UUID.randomUUID();

        TestSession session = TestSession.builder()
                .id(sessionId)
                .userId(userId)
                .type(TestSessionType.CUSTOM)
                .status(TestSessionStatus.ACTIVE)
                .sourceType(TestSessionSourceType.USER_SET)
                .build();

        TestItem firstItem = TestItem.builder()
                .id(firstItemId)
                .testSessionId(sessionId)
                .userVocabId(UUID.randomUUID())
                .questionType(QuestionType.LISTEN_AND_CHOOSE)
                .position(1)
                .status(TestItemStatus.PENDING)
                .build();
        TestItem secondItem = TestItem.builder()
                .id(secondItemId)
                .testSessionId(sessionId)
                .userVocabId(UUID.randomUUID())
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .position(2)
                .status(TestItemStatus.PENDING)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("hash")
                .displayName("User")
                .build()));
        when(testSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(testItemRepository.findByTestSessionIdOrderByPositionAsc(sessionId)).thenReturn(List.of(firstItem, secondItem));

        AppException ex = assertThrows(AppException.class, () -> service.submitAllAnswers(
                userId,
                sessionId,
                List.of(new SubmitTestSessionAnswersRequest.ItemAnswer(firstItemId, "1", 900))
        ));

        assertEquals("MISSING_TEST_ITEM_ANSWERS", ex.getErrorCode());
        assertEquals(Map.of("missingItemIds", List.of(secondItemId)), ex.getDetails());
    }

    @Test
    void createSelectedVocabularySessionShouldRejectWhenAnySelectedVocabularyIsNotInUserList() {
        UUID userId = UUID.randomUUID();
        UUID includedVocabularyId = UUID.randomUUID();
        UUID missingVocabularyId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("hash")
                .displayName("User")
                .build()));
        when(userVocabularyRepository.findByUserIdAndVocabularyIdIn(userId, List.of(includedVocabularyId, missingVocabularyId)))
                .thenReturn(List.of(UserVocabulary.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .vocabularyId(includedVocabularyId)
                        .build()));

        AppException ex = assertThrows(AppException.class, () -> service.createSelectedVocabularySession(
                userId,
                List.of(includedVocabularyId, missingVocabularyId),
                List.of(QuestionType.TRANSLATE_TO_EN)
        ));

        assertEquals("VOCAB_NOT_IN_USER_LIST", ex.getErrorCode());
        assertEquals(Map.of("missingVocabularyIds", List.of(missingVocabularyId)), ex.getDetails());
    }

    @Test
    void createSelectedVocabularySessionShouldRejectUnsupportedQuestionTypes() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("hash")
                .displayName("User")
                .build()));

        AppException ex = assertThrows(AppException.class, () -> service.createSelectedVocabularySession(
                userId,
                List.of(UUID.randomUUID()),
                List.of(QuestionType.TRUE_FALSE)
        ));

        assertEquals("UNSUPPORTED_QUESTION_TYPE", ex.getErrorCode());
        assertEquals(
                Map.of(
                        "unsupportedQuestionTypes", List.of("TRUE_FALSE"),
                        "supportedQuestionTypes", List.of(
                                "MULTIPLE_CHOICE",
                                "LISTEN_AND_CHOOSE",
                                "FILL_MISSING_CHARS",
                                "TRANSLATE_TO_VI",
                                "TRANSLATE_TO_EN",
                                "ACTIVE_RECALL_FULL_WORD"
                        )
                ),
                ex.getDetails()
        );
    }
}
