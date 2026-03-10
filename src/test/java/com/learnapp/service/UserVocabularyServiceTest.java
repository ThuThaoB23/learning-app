package com.learnapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnapp.dto.VocabularyAudioResponse;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.User;
import com.learnapp.entities.UserVocabStatus;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class UserVocabularyServiceTest {

    private final UserVocabularyRepository userVocabularyRepository = mock(UserVocabularyRepository.class);
    private final VocabularyRepository vocabularyRepository = mock(VocabularyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VocabularyAudioService vocabularyAudioService = mock(VocabularyAudioService.class);
    private final SpacedRepetitionService spacedRepetitionService = mock(SpacedRepetitionService.class);
    private final UserActivityLogService userActivityLogService = mock(UserActivityLogService.class);
    private final TopicService topicService = mock(TopicService.class);

    private UserVocabularyService service;

    @BeforeEach
    void setUp() {
        service = new UserVocabularyService(
                userVocabularyRepository,
                vocabularyRepository,
                userRepository,
                vocabularyAudioService,
                spacedRepetitionService,
                userActivityLogService,
                topicService
        );
    }

    @Test
    void listResponsesShouldUseTopicFilterWhenTopicIdProvided() {
        UUID userId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID vocabularyId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        UserVocabulary userVocabulary = UserVocabulary.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .vocabularyId(vocabularyId)
                .status(UserVocabStatus.LEARNING)
                .process(35)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(topicService.getActiveById(topicId)).thenReturn(Topic.builder()
                .id(topicId)
                .name("Travel")
                .slug("travel")
                .status(TopicStatus.ACTIVE)
                .build());
        when(userVocabularyRepository.findByUserIdAndTopicIdAndStatus(userId, topicId, UserVocabStatus.LEARNING, pageable))
                .thenReturn(new PageImpl<>(List.of(userVocabulary), pageable, 1));
        when(vocabularyRepository.findAllById(List.of(vocabularyId))).thenReturn(List.of());
        when(vocabularyAudioService.loadAudioResponses(List.of(vocabularyId))).thenReturn(Map.of(
                vocabularyId,
                List.of(new VocabularyAudioResponse(UUID.randomUUID(), "/audio/travel.mp3", "uk", 1))
        ));

        var result = service.listResponses(userId, UserVocabStatus.LEARNING, topicId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(vocabularyId, result.getContent().get(0).vocabularyId());
        assertEquals(UserVocabStatus.LEARNING, result.getContent().get(0).status());
        verify(topicService).getActiveById(topicId);
        verify(userVocabularyRepository).findByUserIdAndTopicIdAndStatus(userId, topicId, UserVocabStatus.LEARNING, pageable);
    }

    @Test
    void listResponsesShouldKeepExistingBehaviorWhenTopicIdMissing() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(userVocabularyRepository.findByUserIdAndStatus(userId, UserVocabStatus.NEW, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(vocabularyAudioService.loadAudioResponses(List.of())).thenReturn(Map.of());

        var result = service.listResponses(userId, UserVocabStatus.NEW, null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(topicService, never()).getActiveById(any());
        verify(userVocabularyRepository).findByUserIdAndStatus(userId, UserVocabStatus.NEW, pageable);
    }

    @Test
    void listResponsesShouldMapProgressSortAliasToProcess() {
        UUID userId = UUID.randomUUID();
        PageRequest incomingPageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("progress")));
        PageRequest normalizedPageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("process")));

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(userVocabularyRepository.findByUserIdAndStatus(userId, UserVocabStatus.NEW, normalizedPageable))
                .thenReturn(new PageImpl<>(List.of(), normalizedPageable, 0));
        when(vocabularyAudioService.loadAudioResponses(List.of())).thenReturn(Map.of());

        var result = service.listResponses(userId, UserVocabStatus.NEW, null, incomingPageable);

        assertEquals(0, result.getTotalElements());
        verify(userVocabularyRepository).findByUserIdAndStatus(userId, UserVocabStatus.NEW, normalizedPageable);
    }

    @Test
    void listResponsesShouldPropagateNotFoundWhenTopicDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        AppException notFound = new AppException(org.springframework.http.HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found");

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(topicService.getActiveById(topicId)).thenThrow(notFound);

        AppException ex = assertThrows(AppException.class, () -> service.listResponses(userId, null, topicId, pageable));

        assertEquals("TOPIC_NOT_FOUND", ex.getErrorCode());
        verify(userVocabularyRepository, never()).findByUserIdAndTopicIdAndStatus(userId, topicId, null, pageable);
    }

    @Test
    void listResponsesShouldPropagateNotFoundWhenTopicIsInactive() {
        UUID userId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        AppException inactiveHidden = new AppException(org.springframework.http.HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found");

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(topicService.getActiveById(topicId)).thenThrow(inactiveHidden);

        AppException ex = assertThrows(AppException.class, () -> service.listResponses(userId, UserVocabStatus.MASTERED, topicId, pageable));

        assertEquals("TOPIC_NOT_FOUND", ex.getErrorCode());
        verify(userVocabularyRepository, never()).findByUserIdAndTopicIdAndStatus(userId, topicId, UserVocabStatus.MASTERED, pageable);
    }

    private User activeUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("hash")
                .displayName("User")
                .build();
    }
}
