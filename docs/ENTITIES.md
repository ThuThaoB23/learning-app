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
- `progress` (Integer)
- `lastReviewedAt` (LocalDateTime, nullable)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

Enums:
- `UserVocabStatus`: `NEW`, `LEARNING`, `MASTERED`
