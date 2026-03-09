# API Summary

Base URL: `http://localhost:8080`

Auth:
- Public: `/auth/**`, `/docs`, `/v3/api-docs/**`, `/swagger-ui/**`
- Authenticated: all other endpoints
- Admin-only: `/admin/**` (requires `ROLE_ADMIN`)

Common errors (from `GlobalExceptionHandler`):
- `400 VALIDATION_ERROR` – invalid input
- `401 UNAUTHORIZED` – missing/invalid auth
- `403 ACCESS_DENIED` – no permission
- `404 *_NOT_FOUND` – resource missing
- `409 *_EXISTS` – duplicate

Pagination (Spring Pageable):
- Query params: `page`, `size`, `sort`
- Response: Spring `Page<T>` structure

Response schema naming:
- Trong từng endpoint, `Response 200 (XxxResponse)` nghĩa là body tuân theo schema chi tiết ở mục `Response Schemas` bên dưới.

---

## Response Schemas

### `Page<T>`
Spring Data trả về theo dạng:
```json
{
  "content": [],
  "pageable": { "...": "spring pageable metadata" },
  "totalPages": 0,
  "totalElements": 0,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": { "...": "sort metadata" },
  "numberOfElements": 0,
  "first": true,
  "empty": true
}
```
Trong đó `content` là danh sách object `T`.

### `UserResponse`
- `id` (`uuid`)
- `email` (`string`)
- `username` (`string | null`)
- `displayName` (`string`)
- `avatarUrl` (`string | null`)
- `role` (`USER | ADMIN`)
- `status` (`ACTIVE | INACTIVE | BANNED | PENDING_VERIFICATION`)
- `locale` (`string | null`)
- `timeZone` (`string | null`)
- `dailyGoal` (`number | null`)
- `preferences` (`object | null`)
- `lastLoginAt` (`datetime | null`)
- `createdAt` (`datetime`)
- `updatedAt` (`datetime`)

### `LoginResponse`
- `accessToken` (`string`)
- `tokenType` (`string`, thường là `Bearer`)
- `expiresInSeconds` (`number`)
- `user` (`UserResponse`)

### `TopicResponse`
- `id` (`uuid`)
- `name` (`string`)
- `slug` (`string`)
- `description` (`string | null`)
- `createdAt` (`datetime`)

### `VocabularyResponse`
- `id` (`uuid`)
- `term` (`string`)
- `definition` (`string`)
- `definitionVi` (`string | null`)
- `examples` (`string[]`)
- `audios` (`VocabularyAudioResponse[]`)
- `phonetic` (`string | null`)
- `partOfSpeech` (`string | null`)
- `language` (`string`)
- `status` (`PENDING | APPROVED | REJECTED`)
- `inMyVocab` (`boolean | null`) - trạng thái từ này có nằm trong My Vocab của user hiện tại hay không; `null` nếu endpoint không tính field này
- `createdBy` (`uuid | null`)
- `createdAt` (`datetime`)

### `VocabularyAudioResponse`
- `id` (`uuid`)
- `audioUrl` (`string`) - URL public của file audio đã được hệ thống tải về và upload lên MinIO
- `accent` (`string | null`)
- `position` (`number | null`)

### `VocabularyDetailResponse`
- Toàn bộ field của `VocabularyResponse`
- `topicIds` (`uuid[]`)

### `VocabularyAudioBackfillResponse`
- `language` (`string`)
- `status` (`PENDING | APPROVED | REJECTED | null`)
- `forceRefresh` (`boolean`)
- `batchSize` (`number`)
- `limit` (`number | null`)
- `processed` (`number`)
- `updated` (`number`)
- `skipped` (`number`)
- `failed` (`number`)

### `VocabularyContributionResponse`
- `id` (`uuid`)
- `contributorUserId` (`uuid`)
- `contributorDisplayName` (`string | null`)
- `term` (`string`)
- `definition` (`string`)
- `definitionVi` (`string | null`)
- `examples` (`string[]`)
- `phonetic` (`string | null`)
- `partOfSpeech` (`string | null`)
- `language` (`string`)
- `topicIds` (`uuid[]`)
- `status` (`SUBMITTED | IN_REVIEW | APPROVED | REJECTED | CANCELED`)
- `reviewNote` (`string | null`)
- `rejectReason` (`DUPLICATE | INVALID_DEFINITION | WRONG_LANGUAGE | LOW_QUALITY | INAPPROPRIATE_CONTENT | OTHER | null`)
- `approvedVocabularyId` (`uuid | null`)
- `reviewedBy` (`uuid | null`)
- `reviewedAt` (`datetime | null`)
- `createdAt` (`datetime`)
- `updatedAt` (`datetime`)

### `AdminVocabularyContributionQueueItemResponse`
- `id` (`uuid`)
- `term` (`string`)
- `language` (`string`)
- `partOfSpeech` (`string | null`)
- `contributorUserId` (`uuid`)
- `contributorDisplayName` (`string | null`)
- `status` (`SUBMITTED | IN_REVIEW | APPROVED | REJECTED | CANCELED`)
- `createdAt` (`datetime`)

### `VocabularyContributionReviewLogResponse`
- `id` (`uuid`)
- `action` (`SUBMIT | START_REVIEW | APPROVE | REJECT | REOPEN`)
- `actorUserId` (`uuid`)
- `actorDisplayName` (`string | null`)
- `note` (`string | null`)
- `createdAt` (`datetime`)

### `AdminVocabularyContributionDetailResponse`
- `contribution` (`VocabularyContributionResponse`)
- `reviewLogs` (`VocabularyContributionReviewLogResponse[]`)

### `UserVocabularyResponse`
- `vocabularyId` (`uuid`)
- `term` (`string | null`)
- `audios` (`VocabularyAudioResponse[]`)
- `status` (`NEW | LEARNING | MASTERED`)
- `progress` (`number`, 0..100)
- `lastReviewedAt` (`datetime | null`)
- `createdAt` (`datetime`)
- `updatedAt` (`datetime`)

### `FlashcardDeckBucket`
- `DUE` - từ đã tới hạn ôn
- `WEAK` - từ chưa tới hạn nhưng còn yếu (`progress <= 50`)
- `NEW` - từ chưa có lịch sử review hoặc chưa có `nextDueAt`
- `REVIEW` - từ còn lại được fill thêm theo độ ưu tiên

### `FlashcardDeckGroupResponse`
- `bucket` (`DUE | WEAK | NEW | REVIEW`)
- `count` (`number`)

### `FlashcardItemResponse`
- `userVocabularyId` (`uuid`)
- `vocabularyId` (`uuid`)
- `bucket` (`DUE | WEAK | NEW | REVIEW`)
- `term` (`string`)
- `definition` (`string`)
- `definitionVi` (`string | null`)
- `examples` (`string[]`)
- `audios` (`VocabularyAudioResponse[]`)
- `phonetic` (`string | null`)
- `partOfSpeech` (`string | null`)
- `language` (`string`)
- `status` (`NEW | LEARNING | MASTERED`)
- `progress` (`number`, 0..100)
- `lastReviewedAt` (`datetime | null`)
- `nextDueAt` (`datetime | null`)

### `FlashcardDeckResponse`
- `requestedLimit` (`number`)
- `totalItems` (`number`)
- `groups` (`FlashcardDeckGroupResponse[]`)
- `items` (`FlashcardItemResponse[]`)

### `UserActivityLogResponse`
- `id` (`uuid`)
- `userId` (`uuid`)
- `userDisplayName` (`string | null`)
- `activityType` (`REGISTER_ACCOUNT | COMPLETE_STUDY_SESSION | ADD_MYVOCAB | SUBMIT_VOCAB_CONTRIBUTION | APPROVE_VOCAB_CONTRIBUTION | REJECT_VOCAB_CONTRIBUTION`)
- `targetType` (`ACCOUNT | TEST_SESSION | VOCABULARY | VOCABULARY_CONTRIBUTION | null`)
- `targetId` (`uuid | null`)
- `metadata` (`object/json | null`)
- `createdAt` (`datetime`)

### `TestItemResponse`
- `id` (`uuid`)
- `questionType` (`MULTIPLE_CHOICE | LISTEN_AND_CHOOSE | TRUE_FALSE | FILL_MISSING_CHARS | TRANSLATE_TO_VI | TRANSLATE_TO_EN | ACTIVE_RECALL_FULL_WORD | CONTEXT_GAP`)
- `questionPayload` (`object/json`)  
  Ví dụ tùy loại câu hỏi:
  - multiple choice: `{ "prompt": "...", "options": ["...", "...", "...", "..."] }`
  - listen and choose: `{ "prompt": "Listen and choose the correct word", "audioUrl": "...", "accent": "us", "options": ["...", "...", "...", "..."] }`
  - fill missing: `{ "prompt": "...", "maskedTerm": "a__le" }`
  - translate/recall: `{ "prompt": "...", "hint": "a" }` (`hint` có thể không có)
- `position` (`number`, bắt đầu từ 1)
- `status` (`PENDING | CORRECT | WRONG | SKIPPED`)
- `expected` (`string | null`) - đáp án đúng để review; `null` khi item còn `PENDING` trong session `ACTIVE` (tránh lộ đáp án)
- `userAnswer` (`string | null`) - đáp án user đã gửi cho item đó
- `answeredAt` (`datetime | null`)
- `timeMs` (`number | null`)

### `TestSessionResponse`
- `id` (`uuid`)
- `type` (`DAILY | REVIEW | NEW_WORDS | CUSTOM | SET_PRACTICE`)
- `status` (`ACTIVE | COMPLETED | ABANDONED`)
- `title` (`string | null`)
- `scheduleDate` (`date | null`)
- `createdAt` (`datetime`)
- `startedAt` (`datetime | null`)
- `completedAt` (`datetime | null`)
- `totalItems` (`number`)
- `correctCount` (`number`)
- `wrongCount` (`number`)
- `skippedCount` (`number`)
- `score` (`number`, 0..100)
- `items` (`TestItemResponse[]`)

### `SubmitTestItemAnswerResponse`
- `itemId` (`uuid`)
- `status` (`CORRECT | WRONG | PENDING | SKIPPED`)
- `correct` (`boolean`)
- `expected` (`string`)
- `feedback` (`string`)
- `process` (`number`, 0..100)
- `nextDueAt` (`datetime | null`)
- `streak` (`number`)
- `rightCount` (`number`)
- `wrongCount` (`number`)

### `VocabularyImportResultResponse`
- `totalRows` (`number`)
- `importedRows` (`number`)
- `failedRows` (`number`)
- `errors` (`VocabularyImportErrorResponse[]`)

### `VocabularyImportErrorResponse`
- `row` (`number`)
- `message` (`string`)

---

## Auth

### `POST /auth/register` (Public)
Create a user account.

Body (`RegisterRequest`):
```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "User A"
}
```

Response `201` (`UserResponse`):
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "username": null,
  "displayName": "User A",
  "avatarUrl": null,
  "role": "USER",
  "status": "ACTIVE",
  "locale": null,
  "timeZone": null,
  "dailyGoal": null,
  "preferences": null,
  "lastLoginAt": null,
  "createdAt": "2026-02-04T09:00:00",
  "updatedAt": "2026-02-04T09:00:00"
}
```

### `POST /auth/login` (Public)
Login and receive JWT.

Body (`LoginRequest`):
```json
{
  "identifier": "user@example.com",
  "password": "secret123"
}
```
Notes:
- `identifier` accepts either `email` or `username`
- For backward compatibility, payload cũ dùng field `email` vẫn hoạt động

Response `200` (`LoginResponse`):
```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": { "id": "uuid", "email": "user@example.com", "role": "USER", "status": "ACTIVE" }
}
```

---

## User Profile

### `GET /me` (Auth)
Get current user profile.

Response `200` (`UserResponse`)

### `PATCH /me` (Auth)
Update profile fields.

Body (`UpdateMeRequest`):
```json
{
  "username": "john",
  "displayName": "John",
  "avatarUrl": "https://...",
  "locale": "vi",
  "timeZone": "Asia/Ho_Chi_Minh",
  "dailyGoal": 30
}
```

Response `200` (`UserResponse`)

### `PATCH /me/avatar` (Auth)
Upload avatar image for current user (khuyến nghị dùng endpoint này thay vì set `avatarUrl` thủ công trong `PATCH /me`).

Content-Type:
- `multipart/form-data`

Form-data:
- `file` (required): avatar image

Validation:
- max size: `5MB`
- allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`

Response `200` (`UserResponse`)

Quick test:
```bash
curl -X PATCH "http://localhost:8080/me/avatar" \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/avatar.png"
```

Common errors:
- `400 INVALID_FILE` - thiếu file hoặc file rỗng
- `400 FILE_TOO_LARGE` - vượt quá 5MB
- `400 INVALID_FILE_TYPE` - không đúng định dạng ảnh cho phép
- `500 AVATAR_UPLOAD_FAILED` - upload MinIO thất bại

### `GET /me/activity-logs` (Auth)
Get current user's activity history.

Query:
- `activityType` (optional: `REGISTER_ACCOUNT|COMPLETE_STUDY_SESSION|ADD_MYVOCAB|SUBMIT_VOCAB_CONTRIBUTION|APPROVE_VOCAB_CONTRIBUTION|REJECT_VOCAB_CONTRIBUTION`)
- `targetType` (optional: `ACCOUNT|TEST_SESSION|VOCABULARY|VOCABULARY_CONTRIBUTION`)
- `from` (optional, ISO datetime, ví dụ `2026-02-26T00:00:00`)
- `to` (optional, ISO datetime)
- `page`, `size`, `sort` (mặc định nên dùng `sort=createdAt,desc`)

Response `200` (`Page<UserActivityLogResponse>`)

---

## Admin Users (Admin)

### `GET /admin/users`
Search users (non-deleted).

Query:
- `email` (optional, partial)
- `username` (optional, partial)
- `displayName` (optional, partial)
- `role` (optional: `USER|ADMIN`)
- `status` (optional: `ACTIVE|INACTIVE|BANNED|PENDING_VERIFICATION`)
- `page`, `size`, `sort`

Response `200` (`Page<UserResponse>`)

### `GET /admin/users/export`
Export users to CSV (same filters as search).

Query:
- `email` (optional, partial)
- `username` (optional, partial)
- `displayName` (optional, partial)
- `role` (optional: `USER|ADMIN`)
- `status` (optional: `ACTIVE|INACTIVE|BANNED|PENDING_VERIFICATION`)

Response `200` (`text/csv`) with `Content-Disposition: attachment; filename="users.csv"`

### `POST /admin/users`
Create user account.

Body (`RegisterRequest`) – same as `/auth/register`

Response `201` (`UserResponse`)

### `PATCH /admin/users/{userId}`
Update user.

Body (`AdminUpdateUserRequest`):
```json
{
  "email": "new@example.com",
  "username": "newname",
  "displayName": "New Name",
  "avatarUrl": "https://...",
  "role": "ADMIN",
  "status": "ACTIVE",
  "locale": "vi",
  "timeZone": "Asia/Ho_Chi_Minh",
  "dailyGoal": 30
}
```

Response `200` (`UserResponse`)

### `DELETE /admin/users/{userId}`
Soft delete user.

Response `204 No Content`

### `POST /admin/users/{userId}/reset-password`
Reset user password.

Body (`AdminResetPasswordRequest`):
```json
{
  "newPassword": "newSecret123"
}
```

Response `204 No Content`

### `GET /admin/users/{userId}/activity-logs`
List activity logs of a specific user (admin view).

Query:
- `activityType` (optional: `REGISTER_ACCOUNT|COMPLETE_STUDY_SESSION|ADD_MYVOCAB|SUBMIT_VOCAB_CONTRIBUTION|APPROVE_VOCAB_CONTRIBUTION|REJECT_VOCAB_CONTRIBUTION`)
- `targetType` (optional: `ACCOUNT|TEST_SESSION|VOCABULARY|VOCABULARY_CONTRIBUTION`)
- `from` (optional, ISO datetime)
- `to` (optional, ISO datetime)
- `page`, `size`, `sort`

Response `200` (`Page<UserActivityLogResponse>`)

### `POST /admin/users/{userId}/restore`
Restore soft-deleted user.

Response `200` (`UserResponse`)

---

## Admin Activity Logs (Admin)

### `GET /admin/activity-logs`
List activity logs across all users.

Query:
- `userId` (optional, filter 1 user cụ thể)
- `activityType` (optional: `REGISTER_ACCOUNT|COMPLETE_STUDY_SESSION|ADD_MYVOCAB|SUBMIT_VOCAB_CONTRIBUTION|APPROVE_VOCAB_CONTRIBUTION|REJECT_VOCAB_CONTRIBUTION`)
- `targetType` (optional: `ACCOUNT|TEST_SESSION|VOCABULARY|VOCABULARY_CONTRIBUTION`)
- `from` (optional, ISO datetime)
- `to` (optional, ISO datetime)
- `page`, `size`, `sort`

Response `200` (`Page<UserActivityLogResponse>`)

---

## Topics

### `GET /topics` (Auth)
List active topics.

Query:
- `query` (optional: name or slug, partial)
- `page`, `size`, `sort`

Response `200` (`Page<TopicResponse>`)

### `GET /topics/{id}` (Auth)
Get active topic by id.

Response `200` (`TopicResponse`)

### `GET /topics/{id}/vocab` (Auth)
List approved vocabularies in a topic.

Query:
- `query` (optional)
- `language` (optional)
- `status` (optional: `PENDING|APPROVED|REJECTED`)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)

---

## Admin Topics (Admin)

### `GET /admin/topics`
Search topics (non-deleted).

Query:
- `name` (optional, partial)
- `slug` (optional, partial)
- `status` (optional: `ACTIVE|INACTIVE`)
- `page`, `size`, `sort`

Response `200` (`Page<TopicResponse>`)

### `GET /admin/topics/export`
Export topics to CSV (same filters as search).

Query:
- `name` (optional, partial)
- `slug` (optional, partial)
- `status` (optional: `ACTIVE|INACTIVE`)

Response `200` (`text/csv`) with `Content-Disposition: attachment; filename="topics.csv"`

### `POST /admin/topics`
Create topic.

Body (`CreateTopicRequest`):
```json
{
  "name": "Basic English",
  "description": "Common words for beginners"
}
```

Response `200` (`TopicResponse`)

### `PATCH /admin/topics/{id}`
Update topic.

Body (`UpdateTopicRequest`):
```json
{
  "name": "Basic English",
  "description": "Updated description",
  "status": "ACTIVE"
}
```

Response `200` (`TopicResponse`)

### `DELETE /admin/topics/{id}`
Soft delete topic.

Response `204 No Content`

---

## Vocabulary

### `GET /vocab` (Auth)
Search approved vocabularies for màn thêm mới từ vựng.

Query:
- `query` (optional)
- `topicId` (optional)
- `language` (optional)
- `status` (optional: `PENDING|APPROVED|REJECTED`)
- `includeMyVocab` (optional, `boolean`, default `false`) - `true` để vẫn trả về các từ đã có trong My Vocab
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)
Notes:
- Mặc định (`includeMyVocab=false`): vẫn loại các từ đã có trong My Vocab khỏi kết quả.
- Khi `includeMyVocab=true`: kết quả có thể chứa cả từ đã có trong My Vocab, và dùng `inMyVocab` để phân biệt.
- `VocabularyResponse` includes `definitionVi`, `examples: [string]`, and `audios: [VocabularyAudioResponse]`.

### `GET /vocab/{id}` (Auth)
Get approved vocabulary by id.

Response `200` (`VocabularyResponse`)
Note: `VocabularyResponse` includes `definitionVi`, `examples: [string]`, and `audios: [VocabularyAudioResponse]`.

### `POST /vocab/contributions` (Auth)
Submit a new vocabulary contribution (goes to admin review queue).

Body (`CreateVocabularyRequest`):
```json
{
  "term": "apple",
  "definition": "A fruit...",
  "definitionVi": "Một loại trái cây...",
  "examples": ["I eat an apple.", "Apple is tasty."],
  "phonetic": "ˈæp.əl",
  "partOfSpeech": "noun",
  "language": "en",
  "topicIds": ["uuid", "uuid"]
}
```

Response `200` (`VocabularyContributionResponse`)

### `POST /vocab/{id}/audio` (Auth)
Upload một file audio thủ công cho vocabulary đã duyệt.

Content-Type:
- `multipart/form-data`

Form-data:
- `file` (required): audio file
- `accent` (optional): ví dụ `us`, `uk`

Validation:
- max size: `10MB`
- allowed types: `audio/mpeg`, `audio/mp3`, `audio/wav`, `audio/x-wav`, `audio/ogg`, `application/ogg`, `audio/webm`

Response `200` (`VocabularyResponse`)

Notes:
- Chỉ áp dụng cho vocabulary đang `APPROVED`.
- File sẽ được upload lên MinIO của hệ thống; `audios[].audioUrl` trả về là URL MinIO.
- Upload thủ công sẽ thêm một audio mới vào cuối danh sách hiện có, không ghi đè audio cũ.

---

## User Vocabulary

### `GET /me/vocab` (Auth)
List user vocab list.

Query:
- `status` (optional: `NEW|LEARNING|MASTERED`)
- `page`, `size`, `sort`

Response `200` (`Page<UserVocabularyResponse>`)
Note: `UserVocabularyResponse` includes `term` and `audios`.

### `GET /me/vocab/flashcards` (Auth)
Build a flashcard deck from current user's my-vocab list.

Query:
- `limit` (optional, default `20`, min `1`, max `100`)

Response `200` (`FlashcardDeckResponse`)

Notes:
- Deck ưu tiên chọn từ theo nhóm `DUE`, `WEAK`, `NEW`, sau đó fill thêm `REVIEW` theo `priorityScore`.
- `groups[]` cho biết số lượng item trong từng bucket được trả về ở `items[]`.
- Hệ thống lưu lịch sử 2 deck gần nhất xuống DB và cố gắng tránh lặp lại các `userVocabularyId` vừa trả trong khoảng 30 phút, nếu còn đủ ứng viên thay thế.
- Có thể trả `400 NO_USER_VOCAB` nếu user chưa có từ nào trong my vocab.
- Có thể trả `400 NO_ELIGIBLE_VOCAB` nếu không còn vocabulary `APPROVED` hợp lệ để build deck.

### `GET /me/vocab/contributions` (Auth)
List vocabulary contributions submitted by current user.

Query:
- `status` (optional: `SUBMITTED|IN_REVIEW|APPROVED|REJECTED|CANCELED`)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyContributionResponse>`)

### `POST /me/vocab` (Auth)
Add vocab to personal list.

Body (`AddUserVocabularyRequest`):
```json
{
  "vocabularyId": "uuid"
}
```

Response `200` (`UserVocabularyResponse`)

### `PATCH /me/vocab/{vocabularyId}` (Auth)
Update learning status/progress.

Body (`UpdateUserVocabularyRequest`):
```json
{
  "status": "LEARNING",
  "progress": 60
}
```

Response `200` (`UserVocabularyResponse`)

### `DELETE /me/vocab/{vocabularyId}` (Auth)
Remove vocab from list.

Response `204 No Content`

---

## User Test Sessions

### `POST /me/sessions/daily` (Auth)
Create daily session (or return active daily session for today).

Response `200` (`TestSessionResponse`)
Note: Response có `items[]`. Mỗi item có field `expected`, nhưng item `PENDING` trong session `ACTIVE` sẽ trả `expected = null`.

### `POST /me/sessions/topic` (Auth)
Create a topic-based practice session from one or more topics.

Body (`CreateTopicSessionRequest`):
```json
{
  "topicIds": [
    "7fffd9da-b2bb-4f7a-8f95-ec9827ede9e9",
    "d872b2ee-31c1-4f5d-a7ab-77f32f2dff4a"
  ],
  "totalItems": 30
}
```

Response `200` (`TestSessionResponse`)
Note:
- `totalItems` optional, mặc định `20` nếu không gửi.
- Nếu `totalItems` lớn hơn số từ vựng người dùng có trong các topic đã chọn thì hệ thống lấy tất cả.

### `GET /me/sessions/{sessionId}` (Auth)
Get test session detail with ordered items.

Response `200` (`TestSessionResponse`)
Note: `items[].expected` được trả cho item đã có kết quả (`CORRECT|WRONG|SKIPPED`) hoặc khi session không còn `ACTIVE` (phục vụ review).

### `POST /me/sessions/{sessionId}/items/{itemId}/answer` (Auth)
Submit answer for one test item.

Body (`SubmitTestItemAnswerRequest`):
```json
{
  "answer": "apple",
  "timeMs": 3200
}
```

Response `200` (`SubmitTestItemAnswerResponse`)

### `POST /me/sessions/{sessionId}/answers` (Auth)
Submit answers for all `PENDING` items in one request.

Body (`SubmitTestSessionAnswersRequest`):
```json
{
  "answers": [
    {
      "itemId": "c9ec91ff-860f-483a-a7fd-56f8f5e9c21a",
      "answer": "apple",
      "timeMs": 3200
    },
    {
      "itemId": "da7a7a4c-5414-4d93-954f-06f8de001eb5",
      "answer": "banana",
      "timeMs": 2800
    }
  ]
}
```

Response `200` (`SubmitTestSessionAnswersResponse`)
Note:
- Không được trùng `itemId`.
- Item `PENDING` không có trong `answers` sẽ được tính là trả lời sai.

### `POST /me/sessions/{sessionId}/complete` (Auth)
Mark active session as completed.

Response `200` (`TestSessionResponse`)
Note: Item `PENDING` khi complete sẽ được chấm là `WRONG`.

### `POST /me/sessions/{sessionId}/abandon` (Auth)
Mark active session as abandoned.

Response `200` (`TestSessionResponse`)

---

## Admin Vocabulary (Admin)

### `GET /admin/vocab`
Search vocabularies for moderation.

Query:
- `query` (optional)
- `topicId` (optional)
- `language` (optional)
- `status` (optional: `PENDING|APPROVED|REJECTED`)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)

### `GET /admin/vocab/export`
Export vocabularies to CSV (same filters as search).

Query:
- `query` (optional)
- `topicId` (optional)
- `language` (optional)
- `status` (optional: `PENDING|APPROVED|REJECTED`)

Response `200` (`text/csv`) with `Content-Disposition: attachment; filename="vocabularies.csv"`

### `GET /admin/vocab/{id}`
Get vocabulary detail by id (including topic links).

Response `200` (`VocabularyDetailResponse`)
Note: `VocabularyDetailResponse` includes `definitionVi`, `examples: [string]`, `audios: [VocabularyAudioResponse]`, and `topicIds: [uuid]`.

### `PATCH /admin/vocab/{id}`
Update vocabulary fields.

Body (`UpdateVocabularyRequest`):
```json
{
  "term": "apple",
  "definition": "A fruit...",
  "definitionVi": "Một loại trái cây...",
  "examples": [
    { "id": "f2a8c9f2-7b6e-4bc3-9a31-11b8c6cb9f12", "value": "I eat an apple every day." },
    { "value": "Green apples are sour." }
  ],
  "phonetic": "ˈæp.əl",
  "partOfSpeech": "noun",
  "language": "en",
  "status": "APPROVED",
  "topicIds": [
    "3f6d4c1c-1e5a-4b9d-8f77-3b9f1c7a2101",
    "8d2b1f0a-6c9b-4d31-ae92-2f4f9e00b8ad"
  ]
}
```

Response `200` (`VocabularyResponse`)
Notes:
- `examples` is optional. If provided, it replaces current examples:
  - item with `id` => update existing example
  - item without `id` => create new example
  - existing examples not present in payload => removed
- `topicIds` is optional. If provided, it replaces current topic links (add/remove as needed).

### `PATCH /admin/vocab/{id}/approve`
Approve pending vocabulary.

Response `200` (`VocabularyResponse`)

### `PATCH /admin/vocab/{id}/reject`
Reject pending vocabulary.

Response `200` (`VocabularyResponse`)

### `POST /admin/vocab/{id}/audio/refresh`
Fetch và cập nhật lại audio cho một vocabulary đã tồn tại.

Response `200` (`VocabularyResponse`)

Notes:
- Chỉ hỗ trợ vocabulary có `language = en`.
- Nếu API ngoài đang cooldown hoặc lỗi tạm thời, response vẫn trả về vocabulary hiện tại và giữ nguyên audio cũ đã có.
- Khi refresh thành công, audio mới sẽ được sinh từ TTS server `POST /tts`, upload lên MinIO của hệ thống, rồi `audioUrl` trả về sẽ là URL MinIO.

### `DELETE /admin/vocab/{id}/audio/{audioId}`
Xoá một audio cụ thể khỏi vocabulary.

Response `204 No Content`

Notes:
- `audioId` phải thuộc đúng vocabulary `{id}`.
- Nếu xoá thành công, record audio trong DB sẽ bị xoá và object tương ứng trên MinIO sẽ được dọn.
- Các audio còn lại sẽ được đánh lại `position` theo thứ tự `1..n`.

### `POST /admin/vocab/import`
Bulk import vocabularies from CSV.

Content-Type:
- `multipart/form-data`

Form-data:
- `file` (required): CSV file

CSV columns:
- `term` (required)
- `definition` (required)
- `language` (required)
- `definitionVi` (optional)
- `examples` (optional, pipe `|` separated)
- `phonetic` (optional)
- `partOfSpeech` (optional)
- `topics` (optional, topic names separated by `|`, alias: `topicNames`)
- `status` (optional: `PENDING|APPROVED|REJECTED`, default `APPROVED`)

Response `200` (`VocabularyImportResultResponse`)
```json
{
  "totalRows": 2,
  "importedRows": 1,
  "failedRows": 1,
  "errors": [
    { "row": 2, "message": "Vocabulary already exists" }
  ]
}
```

CSV sample file:
- `docs/samples/vocabulary-import-sample.csv`

Topic mapping behavior:
- If topic name already exists, system links vocab to that topic.
- If topic name does not exist, system creates a new topic and links it automatically.

Quick test:
```bash
curl -X POST "http://localhost:8080/admin/vocab/import" \
  -H "Authorization: Bearer <token>" \
  -F "file=@docs/samples/vocabulary-import-sample.csv"
```

Notes:
- Với vocabulary `language = en`, hệ thống sẽ tự gọi TTS server `POST /tts` với body `{"text":"<word>"}`, nhận file WAV, upload lên MinIO, rồi lưu URL MinIO vào `audios` sau khi import thành công.

### `POST /admin/vocab/audio/backfill`
Backfill audio cho vocabulary cũ theo batch.

Query:
- `language` (optional, default `en`)
- `status` (optional: `PENDING|APPROVED|REJECTED`)
- `forceRefresh` (optional, `boolean`, default `false`)
- `batchSize` (optional, `number`, default `100`, range `1..500`)
- `limit` (optional, `number`)

Behavior:
- `forceRefresh=false`: chỉ xử lý vocab chưa có audio.
- `forceRefresh=true`: quét lại cả vocab đã có audio và thay thế audio hiện tại nếu fetch thành công.
- Hiện tại chỉ hỗ trợ `language=en`.
- Nếu API ngoài lỗi tạm thời, hệ thống không xóa audio cũ đã có.
- Khi tạo audio thành công, hệ thống upload audio lên MinIO và lưu URL MinIO vào DB.

Response `200` (`VocabularyAudioBackfillResponse`)

Quick test:
```bash
curl -X POST "http://localhost:8080/admin/vocab/audio/backfill?language=en&status=APPROVED&batchSize=100&limit=500" \
  -H "Authorization: Bearer <token>"
```

### `DELETE /admin/vocab/{id}`
Soft delete vocabulary.

Response `204 No Content`

---

## Admin Vocabulary Contributions (Admin)

### `GET /admin/vocab-contributions`
List user vocabulary contributions for admin review queue.

Query:
- `query` (optional: search by term/definition)
- `language` (optional)
- `status` (optional: `SUBMITTED|IN_REVIEW|APPROVED|REJECTED|CANCELED`)
- `page`, `size`, `sort`

Response `200` (`Page<AdminVocabularyContributionQueueItemResponse>`)

### `GET /admin/vocab-contributions/{id}`
Get contribution detail with examples, topicIds, and review logs.

Response `200` (`AdminVocabularyContributionDetailResponse`)

### `PATCH /admin/vocab-contributions/{id}/approve`
Approve contribution and create an approved vocabulary entry.

Body (`ApproveVocabularyContributionRequest`) (optional):
```json
{
  "reviewNote": "Looks good"
}
```

Response `200` (`VocabularyContributionResponse`)

### `PATCH /admin/vocab-contributions/{id}/reject`
Reject contribution with reason.

Body (`RejectVocabularyContributionRequest`):
```json
{
  "rejectReason": "DUPLICATE",
  "reviewNote": "Already exists in system"
}
```

Response `200` (`VocabularyContributionResponse`)
