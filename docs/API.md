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
  "email": "user@example.com",
  "password": "secret123"
}
```

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

### `POST /admin/users/{userId}/restore`
Restore soft-deleted user.

Response `200` (`UserResponse`)

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
Search approved vocabularies.

Query:
- `query` (optional)
- `topicId` (optional)
- `language` (optional)
- `status` (optional: `PENDING|APPROVED|REJECTED`)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)
Note: `VocabularyResponse` includes `definitionVi` and `examples: [string]`.

### `GET /vocab/{id}` (Auth)
Get approved vocabulary by id.

Response `200` (`VocabularyResponse`)
Note: `VocabularyResponse` includes `definitionVi` and `examples: [string]`.

### `POST /vocab/contributions` (Auth)
Submit a new vocabulary contribution (PENDING).

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

Response `200` (`VocabularyResponse`)

---

## User Vocabulary

### `GET /me/vocab` (Auth)
List user vocab list.

Query:
- `status` (optional: `NEW|LEARNING|MASTERED`)
- `page`, `size`, `sort`

Response `200` (`Page<UserVocabularyResponse>`)

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
Note: `VocabularyDetailResponse` includes `definitionVi`, `examples: [string]`, and `topicIds: [uuid]`.

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

### `DELETE /admin/vocab/{id}`
Soft delete vocabulary.

Response `204 No Content`
