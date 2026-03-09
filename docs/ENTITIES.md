# Entities Summary

Tổng hợp các đối tượng (entity) và thuộc tính hiện có trong ứng dụng.

---

## User (`users`)
**Entity:** `com.learnapp.entities.User`

Fields:
- `id` (UUID)
- `email` (String)
- `username` (String, nullable)
- `passwordHash` (String)
- `displayName` (String)
- `avatarUrl` (String, nullable)
- `role` (UserRole)
- `status` (UserStatus)
- `locale` (String, nullable)
- `timeZone` (String, nullable)
- `dailyGoal` (Integer, nullable)
- `preferences` (Map<String, Object>, JSON)
- `lastLoginAt` (LocalDateTime, nullable)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)
- `deletedAt` (LocalDateTime, nullable)

Enums:
- `UserRole`: `USER`, `ADMIN`
- `UserStatus`: `ACTIVE`, `INACTIVE`, `BANNED`, `PENDING_VERIFICATION`

---

## Topic (`topics`)
**Entity:** `com.learnapp.entities.Topic`

Fields:
- `id` (UUID)
- `name` (String)
- `slug` (String)
- `description` (String, nullable)
- `status` (TopicStatus)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)
- `deletedAt` (LocalDateTime, nullable)

Enums:
- `TopicStatus`: `ACTIVE`, `INACTIVE`

---

## Vocabulary (`vocabularies`)
**Entity:** `com.learnapp.entities.Vocabulary`

Fields:
- `id` (UUID)
- `term` (String)
- `termNormalized` (String)
- `definition` (String)
- `definitionVi` (String, nullable)
- `phonetic` (String, nullable)
- `partOfSpeech` (String, nullable)
- `language` (String)
- `status` (VocabularyStatus)
- `createdBy` (UUID, nullable)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)
- `deletedAt` (LocalDateTime, nullable)

Enums:
- `VocabularyStatus`: `PENDING`, `APPROVED`, `REJECTED`

---

## VocabularyExample (`vocabulary_examples`)
**Entity:** `com.learnapp.entities.VocabularyExample`

Fields:
- `id` (UUID)
- `vocabularyId` (UUID)
- `example` (String)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

---

## VocabularyAudio (`vocabulary_audios`)
**Entity:** `com.learnapp.entities.VocabularyAudio`

Fields:
- `id` (UUID)
- `vocabularyId` (UUID)
- `audioUrl` (String, URL public của object audio đã upload lên MinIO)
- `accent` (String, nullable)
- `position` (Integer, nullable)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

Purpose:
- Lưu nhiều audio phát âm cho một vocabulary
- Dữ liệu hiện được tự động sinh từ TTS server nội bộ cho vocabulary tiếng Anh
- File audio được tải về và lưu lại trên MinIO của hệ thống trước khi ghi `audioUrl` vào DB
- Hệ thống cũng hỗ trợ user upload audio thủ công cho vocabulary đã duyệt khi nguồn ngoài không có sound

---

## TopicVocabulary (`topic_vocabularies`)
**Entity:** `com.learnapp.entities.TopicVocabulary`

Composite Key:
- `topicId` (UUID)
- `vocabularyId` (UUID)

Fields:
- `createdAt` (LocalDateTime)

---

## UserVocabulary (`user_vocabularies`)
**Entity:** `com.learnapp.entities.UserVocabulary`

Fields:
- `id` (UUID)
- `userId` (UUID)
- `vocabularyId` (UUID)
- `status` (UserVocabStatus)
- `process` (Integer)
- `lastReviewedAt` (LocalDateTime, nullable)
- `nextDueAt` (LocalDateTime, nullable)
- `streak` (Integer)
- `rightCount` (Integer)
- `wrongCount` (Integer)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

Enums:
- `UserVocabStatus`: `NEW`, `LEARNING`, `MASTERED`

---

## UserFlashcardDeckHistory (`user_flashcard_deck_histories`)
**Entity:** `com.learnapp.entities.UserFlashcardDeckHistory`

Fields:
- `id` (UUID)
- `userId` (UUID)
- `servedVocabularyIds` (JsonNode, JSON array of `userVocabularyId`)
- `totalItems` (Integer)
- `createdAt` (LocalDateTime)

Purpose:
- Lưu 2 deck flashcard gần nhất theo user để API `/me/vocab/flashcards` tránh lặp lại các từ vừa được trả ở các lần gọi liên tiếp, kể cả sau khi app restart.

---

## TestSession (`test_sessions`)
**Entity:** `com.learnapp.entities.TestSession`

Fields:
- `id` (UUID)
- `userId` (UUID)
- `type` (TestSessionType)
- `status` (TestSessionStatus)
- `title` (String, nullable)
- `scheduleDate` (LocalDate, nullable)
- `sourceType` (TestSessionSourceType)
- `sourceRefId` (UUID, nullable)
- `sourceParams` (JsonNode, nullable)
- `createdAt` (LocalDateTime)
- `startedAt` (LocalDateTime, nullable)
- `completedAt` (LocalDateTime, nullable)
- `totalItems` (Integer)
- `correctCount` (Integer)
- `wrongCount` (Integer)
- `skippedCount` (Integer)
- `score` (Integer, 0..100)

Enums:
- `TestSessionType`: `DAILY`, `REVIEW`, `NEW_WORDS`, `CUSTOM`, `SET_PRACTICE`
- `TestSessionStatus`: `ACTIVE`, `COMPLETED`, `ABANDONED`
- `TestSessionSourceType`: `DAILY_RULE`, `FILTER`, `MANUAL_PICK`, `USER_SET`

---

## TestItem (`test_items`)
**Entity:** `com.learnapp.entities.TestItem`

Fields:
- `id` (UUID)
- `testSessionId` (UUID)
- `userVocabId` (UUID)
- `questionType` (QuestionType)
- `questionPayload` (JsonNode)
- `position` (Integer)
- `status` (TestItemStatus)
- `userAnswer` (String, nullable)
- `answeredAt` (LocalDateTime, nullable)
- `timeMs` (Integer, nullable)
- `createdAt` (LocalDateTime)

Enums:
- `QuestionType`: `MULTIPLE_CHOICE`, `LISTEN_AND_CHOOSE`, `TRUE_FALSE`, `FILL_MISSING_CHARS`, `TRANSLATE_TO_VI`, `TRANSLATE_TO_EN`, `ACTIVE_RECALL_FULL_WORD`, `CONTEXT_GAP`
- `TestItemStatus`: `PENDING`, `CORRECT`, `WRONG`, `SKIPPED`

---

## Related Non-Entity Objects Added

Các object sau không phải JPA entity nhưng vừa được thêm để hỗ trợ audio:

- `com.learnapp.dto.VocabularyAudioResponse`
- `com.learnapp.dto.VocabularyAudioBackfillResponse`
- `com.learnapp.service.VocabularyAudioService`
- `com.learnapp.service.VocabularyAudioStorageService`
- `com.learnapp.config.TextToSpeechProperties`
