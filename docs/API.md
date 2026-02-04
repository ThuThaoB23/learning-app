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
List users (non-deleted).

Query: `page`, `size`, `sort`

Response `200` (`Page<UserResponse>`)

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

Query: `page`, `size`, `sort`

Response `200` (`Page<TopicResponse>`)

### `GET /topics/{id}` (Auth)
Get active topic by id.

Response `200` (`TopicResponse`)

### `GET /topics/{id}/vocab` (Auth)
List approved vocabularies in a topic.

Query:
- `query` (optional)
- `language` (optional)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)

---

## Vocabulary

### `GET /vocab` (Auth)
Search approved vocabularies.

Query:
- `query` (optional)
- `topicId` (optional)
- `language` (optional)
- `page`, `size`, `sort`

Response `200` (`Page<VocabularyResponse>`)

### `GET /vocab/{id}` (Auth)
Get approved vocabulary by id.

Response `200` (`VocabularyResponse`)

### `POST /vocab/contributions` (Auth)
Submit a new vocabulary contribution (PENDING).

Body (`CreateVocabularyRequest`):
```json
{
  "term": "apple",
  "definition": "A fruit...",
  "example": "I eat an apple.",
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

### `PATCH /admin/vocab/{id}/approve`
Approve pending vocabulary.

Response `200` (`VocabularyResponse`)

### `PATCH /admin/vocab/{id}/reject`
Reject pending vocabulary.

Response `200` (`VocabularyResponse`)
