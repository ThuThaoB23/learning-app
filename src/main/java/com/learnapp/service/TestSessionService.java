package com.learnapp.service;

import com.learnapp.dto.SubmitTestItemAnswerResponse;
import com.learnapp.dto.SubmitTestSessionAnswersRequest;
import com.learnapp.dto.SubmitTestSessionAnswersResponse;
import com.learnapp.dto.TestItemResponse;
import com.learnapp.dto.TestSessionResponse;
import com.learnapp.entities.TestItem;
import com.learnapp.entities.TestItemStatus;
import com.learnapp.entities.TestSession;
import com.learnapp.entities.TestSessionSourceType;
import com.learnapp.entities.TestSessionStatus;
import com.learnapp.entities.TestSessionType;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.User;
import com.learnapp.entities.UserVocabStatus;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TestItemRepository;
import com.learnapp.repository.TestSessionRepository;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TestSessionService {

    private static final int DAILY_SIZE = 20;
    private static final int DAILY_DUE_QUOTA = 14;
    private static final int DAILY_WEAK_QUOTA = 4;
    private static final int DAILY_NEW_QUOTA = 2;
    private static final int TOPIC_DEFAULT_SIZE = 20;
    private static final int WEAK_THRESHOLD = 50;
    private static final int DISTRACTOR_POOL_SIZE = 200;
    private static final int DISTRACTOR_WINDOW_SIZE = 40;

    private final TestSessionRepository testSessionRepository;
    private final TestItemRepository testItemRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final TopicVocabularyRepository topicVocabularyRepository;
    private final UserRepository userRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final QuestionEngineService questionEngineService;
    private final UserActivityLogService userActivityLogService;

    public TestSessionService(
            TestSessionRepository testSessionRepository,
            TestItemRepository testItemRepository,
            UserVocabularyRepository userVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            TopicRepository topicRepository,
            TopicVocabularyRepository topicVocabularyRepository,
            UserRepository userRepository,
            SpacedRepetitionService spacedRepetitionService,
            QuestionEngineService questionEngineService,
            UserActivityLogService userActivityLogService
    ) {
        this.testSessionRepository = testSessionRepository;
        this.testItemRepository = testItemRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.topicRepository = topicRepository;
        this.topicVocabularyRepository = topicVocabularyRepository;
        this.userRepository = userRepository;
        this.spacedRepetitionService = spacedRepetitionService;
        this.questionEngineService = questionEngineService;
        this.userActivityLogService = userActivityLogService;
    }

    public TestSessionResponse createDailySession(UUID userId) {
        User user = ensureUserNotDeleted(userId);
        ZoneId zoneId = resolveZone(user.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);

        TestSession existing = testSessionRepository.findByUserIdAndTypeAndScheduleDateAndStatus(
                        userId,
                        TestSessionType.DAILY,
                        today,
                        TestSessionStatus.ACTIVE
                )
                .orElse(null);
        if (existing != null) {
            return toResponse(existing, testItemRepository.findByTestSessionIdOrderByPositionAsc(existing.getId()));
        }

        List<UserVocabulary> allItems = userVocabularyRepository.findByUserId(userId);
        if (allItems.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_USER_VOCAB", "No vocabulary found in user list");
        }

        List<UserVocabulary> selected = selectDailyItems(allItems, today, zoneId);
        if (selected.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_ELIGIBLE_ITEMS", "No eligible items for daily session");
        }

        TestSession session = TestSession.builder()
                .userId(userId)
                .type(TestSessionType.DAILY)
                .status(TestSessionStatus.ACTIVE)
                .title("Daily Session " + today)
                .scheduleDate(today)
                .sourceType(TestSessionSourceType.DAILY_RULE)
                .startedAt(LocalDateTime.now())
                .build();
        session = testSessionRepository.save(session);

        List<TestItem> items = buildTestItems(selected, session, today, zoneId);
        if (items.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_ELIGIBLE_VOCAB", "No approved vocabulary available");
        }
        items = testItemRepository.saveAll(items);
        recalculateSessionStats(session, items);

        return toResponse(session, items);
    }

    public TestSessionResponse createTopicSession(
            UUID userId,
            List<UUID> topicIds,
            Integer totalItems
    ) {
        User user = ensureUserNotDeleted(userId);
        ZoneId zoneId = resolveZone(user.getTimeZone());
        LocalDate today = LocalDate.now(zoneId);

        Set<UUID> uniqueTopicIds = new LinkedHashSet<>(topicIds);
        ensureTopicsAreActive(uniqueTopicIds);

        Set<UUID> vocabularyIds = topicVocabularyRepository.findByTopicIdIn(uniqueTopicIds).stream()
                .map(topicVocabulary -> topicVocabulary.getVocabularyId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (vocabularyIds.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_TOPIC_VOCAB", "No vocabulary found in selected topics");
        }

        List<UserVocabulary> userItems = userVocabularyRepository.findByUserIdAndVocabularyIdIn(userId, vocabularyIds);
        if (userItems.isEmpty()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "NO_USER_VOCAB_IN_TOPICS",
                    "No user vocabulary found in selected topics"
            );
        }

        int requestedItems = totalItems == null ? TOPIC_DEFAULT_SIZE : totalItems;
        Collections.shuffle(userItems);
        int limit = Math.min(requestedItems, userItems.size());
        List<UserVocabulary> selected = new ArrayList<>(userItems.subList(0, limit));

        TestSession session = TestSession.builder()
                .userId(userId)
                .type(TestSessionType.CUSTOM)
                .status(TestSessionStatus.ACTIVE)
                .title("Topic Session " + today)
                .scheduleDate(today)
                .sourceType(TestSessionSourceType.FILTER)
                .startedAt(LocalDateTime.now())
                .build();
        session = testSessionRepository.save(session);

        List<TestItem> items = buildTestItems(selected, session, today, zoneId);
        if (items.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "NO_ELIGIBLE_VOCAB", "No approved vocabulary available");
        }
        items = testItemRepository.saveAll(items);
        recalculateSessionStats(session, items);

        return toResponse(session, items);
    }

    @Transactional(readOnly = true)
    public TestSessionResponse getSession(UUID userId, UUID sessionId) {
        TestSession session = findOwnedSession(userId, sessionId);
        List<TestItem> items = testItemRepository.findByTestSessionIdOrderByPositionAsc(sessionId);
        return toResponse(session, items);
    }

    public SubmitTestItemAnswerResponse submitAnswer(
            UUID userId,
            UUID sessionId,
            UUID itemId,
            String answer,
            Integer timeMs
    ) {
        TestSession session = findOwnedSession(userId, sessionId);
        if (session.getStatus() != TestSessionStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "Session is not active");
        }

        TestItem item = testItemRepository.findByIdAndTestSessionId(itemId, sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TEST_ITEM_NOT_FOUND", "Test item not found"));
        if (item.getStatus() != TestItemStatus.PENDING) {
            return buildAlreadyAnsweredResponse(userId, item, answer);
        }

        UserVocabulary userVocabulary = userVocabularyRepository.findByIdAndUserId(item.getUserVocabId(), userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_VOCAB_NOT_FOUND", "User vocabulary not found"));

        QuestionEngineService.GradeResult grade = questionEngineService.grade(item.getQuestionType(), item.getQuestionPayload(), answer);
        LocalDateTime now = LocalDateTime.now();

        item.setStatus(grade.correct() ? TestItemStatus.CORRECT : TestItemStatus.WRONG);
        item.setUserAnswer(answer);
        item.setAnsweredAt(now);
        item.setTimeMs(timeMs);
        testItemRepository.save(item);
        recalculateSessionStats(session, testItemRepository.findByTestSessionIdOrderByPositionAsc(sessionId));

        spacedRepetitionService.applyAttempt(userVocabulary, grade.correct(), now);
        syncStatusByProcess(userVocabulary);
        userVocabularyRepository.save(userVocabulary);

        return new SubmitTestItemAnswerResponse(
                item.getId(),
                item.getStatus(),
                grade.correct(),
                grade.expected(),
                grade.feedback(),
                userVocabulary.getProcess(),
                userVocabulary.getNextDueAt(),
                userVocabulary.getStreak(),
                userVocabulary.getRightCount(),
                userVocabulary.getWrongCount()
        );
    }

    public SubmitTestSessionAnswersResponse submitAllAnswers(
            UUID userId,
            UUID sessionId,
            List<SubmitTestSessionAnswersRequest.ItemAnswer> answers
    ) {
        TestSession session = findOwnedSession(userId, sessionId);
        if (session.getStatus() != TestSessionStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "Session is not active");
        }

        List<TestItem> items = testItemRepository.findByTestSessionIdOrderByPositionAsc(sessionId);
        Map<UUID, TestItem> itemById = new HashMap<>();
        for (TestItem item : items) {
            itemById.put(item.getId(), item);
        }

        Map<UUID, SubmitTestSessionAnswersRequest.ItemAnswer> answerByItemId = new HashMap<>();
        for (SubmitTestSessionAnswersRequest.ItemAnswer answer : answers) {
            if (answerByItemId.putIfAbsent(answer.itemId(), answer) != null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "DUPLICATE_TEST_ITEM_ANSWER", "Duplicate itemId in answers");
            }
        }

        List<TestItem> pendingItems = new ArrayList<>();
        for (TestItem item : items) {
            if (item.getStatus() == TestItemStatus.PENDING) {
                pendingItems.add(item);
            }
        }

        for (SubmitTestSessionAnswersRequest.ItemAnswer answer : answers) {
            TestItem item = itemById.get(answer.itemId());
            if (item == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TEST_ITEM_NOT_IN_SESSION", "Test item does not belong to session");
            }
            if (item.getStatus() != TestItemStatus.PENDING) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TEST_ITEM_ALREADY_ANSWERED", "Test item already answered");
            }
        }

        Map<UUID, UserVocabulary> userVocabularyById = new HashMap<>();
        for (TestItem item : pendingItems) {
            UUID userVocabId = item.getUserVocabId();
            if (userVocabularyById.containsKey(userVocabId)) {
                continue;
            }
            UserVocabulary userVocabulary = userVocabularyRepository.findByIdAndUserId(userVocabId, userId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_VOCAB_NOT_FOUND", "User vocabulary not found"));
            userVocabularyById.put(userVocabId, userVocabulary);
        }

        Map<UUID, SubmitTestItemAnswerResponse> resultByItemId = new HashMap<>();
        for (TestItem item : pendingItems) {
            SubmitTestSessionAnswersRequest.ItemAnswer answer = answerByItemId.get(item.getId());
            boolean answeredByUser = answer != null;
            String submittedAnswer = answeredByUser ? answer.answer() : "";
            UserVocabulary userVocabulary = userVocabularyById.get(item.getUserVocabId());
            QuestionEngineService.GradeResult grade = questionEngineService.grade(
                    item.getQuestionType(),
                    item.getQuestionPayload(),
                    submittedAnswer
            );
            LocalDateTime now = LocalDateTime.now();

            item.setStatus(grade.correct() ? TestItemStatus.CORRECT : TestItemStatus.WRONG);
            item.setUserAnswer(answeredByUser ? answer.answer() : null);
            item.setAnsweredAt(now);
            item.setTimeMs(answeredByUser ? answer.timeMs() : null);

            spacedRepetitionService.applyAttempt(userVocabulary, grade.correct(), now);
            syncStatusByProcess(userVocabulary);

            String feedback = answeredByUser ? grade.feedback() : "No answer provided";

            resultByItemId.put(item.getId(), new SubmitTestItemAnswerResponse(
                    item.getId(),
                    item.getStatus(),
                    grade.correct(),
                    grade.expected(),
                    feedback,
                    userVocabulary.getProcess(),
                    userVocabulary.getNextDueAt(),
                    userVocabulary.getStreak(),
                    userVocabulary.getRightCount(),
                    userVocabulary.getWrongCount()
            ));
        }

        testItemRepository.saveAll(pendingItems);
        userVocabularyRepository.saveAll(userVocabularyById.values());
        recalculateSessionStats(session, items);

        List<SubmitTestItemAnswerResponse> results = pendingItems.stream()
                .sorted(Comparator.comparing(TestItem::getPosition))
                .map(item -> resultByItemId.get(item.getId()))
                .toList();

        return new SubmitTestSessionAnswersResponse(
                session.getId(),
                session.getStatus(),
                session.getTotalItems(),
                session.getCorrectCount(),
                session.getWrongCount(),
                session.getSkippedCount(),
                session.getScore(),
                results
        );
    }

    public TestSessionResponse completeSession(UUID userId, UUID sessionId) {
        TestSession session = findOwnedSession(userId, sessionId);
        if (session.getStatus() == TestSessionStatus.COMPLETED) {
            return toResponse(session, testItemRepository.findByTestSessionIdOrderByPositionAsc(session.getId()));
        }
        if (session.getStatus() == TestSessionStatus.ABANDONED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "Session was abandoned");
        }
        List<TestItem> items = testItemRepository.findByTestSessionIdOrderByPositionAsc(session.getId());
        LocalDateTime now = LocalDateTime.now();
        markPendingAsWrong(userId, items, now);
        if (!items.isEmpty()) {
            testItemRepository.saveAll(items);
        }
        session.setStatus(TestSessionStatus.COMPLETED);
        session.setCompletedAt(now);
        recalculateSessionStats(session, items);
        userActivityLogService.logCompleteStudySession(session);
        return toResponse(session, items);
    }

    public TestSessionResponse abandonSession(UUID userId, UUID sessionId) {
        TestSession session = findOwnedSession(userId, sessionId);
        if (session.getStatus() == TestSessionStatus.ABANDONED) {
            return toResponse(session, testItemRepository.findByTestSessionIdOrderByPositionAsc(session.getId()));
        }
        if (session.getStatus() == TestSessionStatus.COMPLETED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "Session was completed");
        }
        List<TestItem> items = testItemRepository.findByTestSessionIdOrderByPositionAsc(session.getId());
        markPendingAsSkipped(items);
        if (!items.isEmpty()) {
            testItemRepository.saveAll(items);
        }
        session.setStatus(TestSessionStatus.ABANDONED);
        session.setCompletedAt(LocalDateTime.now());
        recalculateSessionStats(session, items);
        return toResponse(session, items);
    }

    private List<UserVocabulary> selectDailyItems(List<UserVocabulary> allItems, LocalDate today, ZoneId zoneId) {
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<UserVocabulary> dueItems = allItems.stream()
                .filter(uv -> uv.getNextDueAt() != null && !uv.getNextDueAt().isAfter(endOfDay))
                .sorted(byPriority(today, zoneId))
                .toList();

        List<UserVocabulary> weakItems = allItems.stream()
                .filter(uv -> nullSafe(uv.getProcess()) <= WEAK_THRESHOLD)
                .sorted(byPriority(today, zoneId))
                .toList();

        List<UserVocabulary> newItems = allItems.stream()
                .filter(uv -> uv.getLastReviewedAt() == null || uv.getNextDueAt() == null)
                .sorted(Comparator.comparing(UserVocabulary::getCreatedAt))
                .toList();

        List<UserVocabulary> picked = new ArrayList<>();
        Set<UUID> pickedIds = new HashSet<>();

        pick(dueItems, DAILY_DUE_QUOTA, pickedIds, picked);
        pick(weakItems, DAILY_WEAK_QUOTA, pickedIds, picked);
        pick(newItems, DAILY_NEW_QUOTA, pickedIds, picked);

        if (picked.size() < DAILY_SIZE) {
            List<UserVocabulary> fallback = new ArrayList<>(allItems);
            fallback.sort(byPriority(today, zoneId));
            pick(fallback, DAILY_SIZE - picked.size(), pickedIds, picked);
        }

        return picked.size() > DAILY_SIZE ? picked.subList(0, DAILY_SIZE) : picked;
    }

    private List<TestItem> buildTestItems(
            List<UserVocabulary> selected,
            TestSession session,
            LocalDate today,
            ZoneId zoneId
    ) {
        List<UUID> vocabIds = selected.stream().map(UserVocabulary::getVocabularyId).toList();
        List<Vocabulary> vocabularies = vocabularyRepository.findByIdInAndStatusAndDeletedAtIsNull(vocabIds, VocabularyStatus.APPROVED);
        Map<UUID, Vocabulary> vocabById = new HashMap<>();
        for (Vocabulary vocabulary : vocabularies) {
            vocabById.put(vocabulary.getId(), vocabulary);
        }

        Map<String, List<Vocabulary>> distractorPoolsByLanguage = buildDistractorPoolsByLanguage(vocabularies);
        Map<String, Integer> distractorOffsetsByLanguage = new HashMap<>();

        List<TestItem> items = new ArrayList<>();
        int position = 1;
        for (UserVocabulary userVocabulary : selected) {
            Vocabulary vocabulary = vocabById.get(userVocabulary.getVocabularyId());
            if (vocabulary == null) {
                continue;
            }
            List<Vocabulary> distractors = selectDistractorCandidates(
                    distractorPoolsByLanguage.get(vocabulary.getLanguage()),
                    vocabulary,
                    distractorOffsetsByLanguage
            );

            QuestionEngineService.GeneratedQuestion generated = questionEngineService.generateQuestion(
                    userVocabulary,
                    vocabulary,
                    session.getType(),
                    today,
                    zoneId,
                    distractors
            );

            items.add(TestItem.builder()
                    .testSessionId(session.getId())
                    .userVocabId(userVocabulary.getId())
                    .questionType(generated.type())
                    .questionPayload(generated.payload())
                    .position(position++)
                    .status(TestItemStatus.PENDING)
                    .build());
        }
        return items;
    }

    private Map<String, List<Vocabulary>> buildDistractorPoolsByLanguage(List<Vocabulary> vocabularies) {
        Set<String> languages = new HashSet<>();
        for (Vocabulary vocabulary : vocabularies) {
            if (vocabulary.getLanguage() != null && !vocabulary.getLanguage().isBlank()) {
                languages.add(vocabulary.getLanguage());
            }
        }

        Map<String, List<Vocabulary>> pools = new HashMap<>();
        for (String language : languages) {
            List<Vocabulary> pool = new ArrayList<>(vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguage(
                    VocabularyStatus.APPROVED,
                    language,
                    PageRequest.of(0, DISTRACTOR_POOL_SIZE)
            ).getContent());
            if (!pool.isEmpty()) {
                Collections.shuffle(pool);
            }
            pools.put(language, pool);
        }
        return pools;
    }

    private List<Vocabulary> selectDistractorCandidates(
            List<Vocabulary> pool,
            Vocabulary answer,
            Map<String, Integer> distractorOffsetsByLanguage
    ) {
        if (pool == null || pool.isEmpty() || answer == null) {
            return List.of();
        }

        String languageKey = answer.getLanguage();
        int start = distractorOffsetsByLanguage.getOrDefault(languageKey, 0);
        int windowSize = Math.min(DISTRACTOR_WINDOW_SIZE, pool.size());

        List<Vocabulary> candidates = new ArrayList<>(windowSize);
        Set<UUID> seenIds = new HashSet<>();
        for (int i = 0; i < pool.size() && candidates.size() < windowSize; i++) {
            Vocabulary candidate = pool.get((start + i) % pool.size());
            if (candidate == null || candidate.getId() == null || candidate.getId().equals(answer.getId())) {
                continue;
            }
            if (!seenIds.add(candidate.getId())) {
                continue;
            }
            candidates.add(candidate);
        }

        int nextOffset = pool.isEmpty() ? 0 : (start + windowSize) % pool.size();
        distractorOffsetsByLanguage.put(languageKey, nextOffset);
        return candidates;
    }

    private Comparator<UserVocabulary> byPriority(LocalDate today, ZoneId zoneId) {
        return Comparator.comparingDouble((UserVocabulary uv) ->
                spacedRepetitionService.priorityScore(uv, today, zoneId)).reversed();
    }

    private void pick(List<UserVocabulary> source, int need, Set<UUID> pickedIds, List<UserVocabulary> result) {
        int added = 0;
        for (UserVocabulary userVocabulary : source) {
            if (added >= need) {
                break;
            }
            if (pickedIds.add(userVocabulary.getId())) {
                result.add(userVocabulary);
                added++;
            }
        }
    }

    private void ensureTopicsAreActive(Set<UUID> topicIds) {
        for (UUID topicId : topicIds) {
            Topic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));
            if (topic.getStatus() != TopicStatus.ACTIVE) {
                throw new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found");
            }
        }
    }

    private TestSession findOwnedSession(UUID userId, UUID sessionId) {
        ensureUserNotDeleted(userId);
        return testSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
    }

    private SubmitTestItemAnswerResponse buildAlreadyAnsweredResponse(UUID userId, TestItem item, String answer) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByIdAndUserId(item.getUserVocabId(), userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_VOCAB_NOT_FOUND", "User vocabulary not found"));
        boolean correct = item.getStatus() == TestItemStatus.CORRECT;
        QuestionEngineService.GradeResult grade = questionEngineService.grade(item.getQuestionType(), item.getQuestionPayload(), answer);
        return new SubmitTestItemAnswerResponse(
                item.getId(),
                item.getStatus(),
                correct,
                grade.expected(),
                correct ? "Already answered correctly" : "Already answered incorrectly",
                userVocabulary.getProcess(),
                userVocabulary.getNextDueAt(),
                userVocabulary.getStreak(),
                userVocabulary.getRightCount(),
                userVocabulary.getWrongCount()
        );
    }

    private TestSessionResponse toResponse(TestSession session, List<TestItem> items) {
        List<TestItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(TestItem::getPosition))
                .map(item -> new TestItemResponse(
                        item.getId(),
                        item.getQuestionType(),
                        questionEngineService.toPlainJsonPayload(item.getQuestionPayload()),
                        item.getPosition(),
                        item.getStatus(),
                        resolveExpectedForResponse(session, item),
                        item.getUserAnswer(),
                        item.getAnsweredAt(),
                        item.getTimeMs()
                ))
                .toList();

        return new TestSessionResponse(
                session.getId(),
                session.getType(),
                session.getStatus(),
                session.getTitle(),
                session.getScheduleDate(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getTotalItems(),
                session.getCorrectCount(),
                session.getWrongCount(),
                session.getSkippedCount(),
                session.getScore(),
                itemResponses
        );
    }

    private void markPendingAsSkipped(List<TestItem> items) {
        for (TestItem item : items) {
            if (item.getStatus() == TestItemStatus.PENDING) {
                item.setStatus(TestItemStatus.SKIPPED);
            }
        }
    }

    private void markPendingAsWrong(UUID userId, List<TestItem> items, LocalDateTime answeredAt) {
        Map<UUID, UserVocabulary> userVocabularyById = new HashMap<>();
        for (TestItem item : items) {
            if (item.getStatus() != TestItemStatus.PENDING) {
                continue;
            }

            UUID userVocabId = item.getUserVocabId();
            UserVocabulary userVocabulary = userVocabularyById.get(userVocabId);
            if (userVocabulary == null) {
                userVocabulary = userVocabularyRepository.findByIdAndUserId(userVocabId, userId)
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_VOCAB_NOT_FOUND", "User vocabulary not found"));
                userVocabularyById.put(userVocabId, userVocabulary);
            }

            item.setStatus(TestItemStatus.WRONG);
            item.setUserAnswer(null);
            item.setAnsweredAt(answeredAt);
            item.setTimeMs(null);

            spacedRepetitionService.applyAttempt(userVocabulary, false, answeredAt);
            syncStatusByProcess(userVocabulary);
        }

        if (!userVocabularyById.isEmpty()) {
            userVocabularyRepository.saveAll(userVocabularyById.values());
        }
    }

    private String resolveExpectedForResponse(TestSession session, TestItem item) {
        boolean canReveal = session.getStatus() != TestSessionStatus.ACTIVE
                || item.getStatus() != TestItemStatus.PENDING;
        if (!canReveal || item.getQuestionPayload() == null) {
            return null;
        }
        String expected = item.getQuestionPayload().path("expected").asText(null);
        return (expected == null || expected.isBlank()) ? null : expected;
    }

    private void recalculateSessionStats(TestSession session, List<TestItem> items) {
        int total = items.size();
        int correct = 0;
        int wrong = 0;
        int skipped = 0;

        for (TestItem item : items) {
            if (item.getStatus() == TestItemStatus.CORRECT) {
                correct++;
            } else if (item.getStatus() == TestItemStatus.WRONG) {
                wrong++;
            } else if (item.getStatus() == TestItemStatus.SKIPPED) {
                skipped++;
            }
        }

        int score = total == 0 ? 0 : Math.round((correct * 100.0f) / total);
        session.setTotalItems(total);
        session.setCorrectCount(correct);
        session.setWrongCount(wrong);
        session.setSkippedCount(skipped);
        session.setScore(score);
        testSessionRepository.save(session);
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

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private void syncStatusByProcess(UserVocabulary userVocabulary) {
        int process = nullSafe(userVocabulary.getProcess());
        if (process >= 90) {
            userVocabulary.setStatus(UserVocabStatus.MASTERED);
            return;
        }
        if (process >= 1) {
            userVocabulary.setStatus(UserVocabStatus.LEARNING);
            return;
        }
        userVocabulary.setStatus(UserVocabStatus.NEW);
    }
}
