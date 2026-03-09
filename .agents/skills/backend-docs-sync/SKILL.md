---
name: backend-docs-sync
description: Keep backend documentation in sync with code changes. Use when changing controllers, request/response DTOs, entities, enums, migrations, repository queries, security-exposed routes, or business logic that changes the public API contract or entity shape. Update docs/API.md for endpoint and schema changes, and update docs/ENTITIES.md for entity, enum, table, or persistence model changes in the same turn.
---

# Backend Docs Sync

Update backend docs in the same task as the code change. Do not leave `docs/API.md` or `docs/ENTITIES.md` stale when the change affects them.

## Trigger Checklist

Use this skill when a change touches any of these areas:
- Controller endpoints, request params, path variables, auth requirements, status codes
- Request/response DTOs or response body shape
- Entity fields, enums, table mappings, indexes, relationships
- Flyway migrations that add, remove, or rename persisted columns or tables
- Repository or service logic that changes API behavior visible to clients

## Required Outputs

- Update `docs/API.md` when API surface or schema changes
- Update `docs/ENTITIES.md` when entity or persistence model changes
- If both changed, update both files in the same turn
- If a touched backend file does not require a doc update, state that explicitly in the final response

## Workflow

1. Inspect the changed backend files and identify whether the change affects API docs, entity docs, or both.
2. Update `docs/API.md` for:
   - new, removed, or renamed endpoints
   - new query params, path params, or request bodies
   - response schema changes
   - auth or permission changes
   - new error codes or important behavior notes
3. Update `docs/ENTITIES.md` for:
   - new, removed, or renamed entities
   - field additions, removals, renames, or nullability changes
   - enum value changes
   - new tables or relationship changes reflected by entities or migrations
4. Keep wording short and concrete. Match the existing docs style instead of inventing a new format.
5. Before finishing, verify that each backend contract change is reflected in at least one docs file.

## File Targets

- API docs: `docs/API.md`
- Entity docs: `docs/ENTITIES.md`

## Final Check

Do not close the task with backend contract changes unreflected in docs.
