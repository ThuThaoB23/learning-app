# Project Instructions

## Documentation Rule

- Use the local skill `backend-docs-sync` whenever a task changes backend API contracts or persistence shapes.
- Treat these changes as documentation-impacting by default: controllers, DTOs, entities, enums, Flyway migrations, repository queries, security-exposed routes, and service logic that changes client-visible behavior.
- In the same turn, update `docs/API.md` for API or schema changes and update `docs/ENTITIES.md` for entity or persistence changes.
- Do not leave those docs stale. If a backend code change truly does not require a docs edit, say so explicitly in the final response.

## Skill Preference

- When a backend task also needs normal Spring Boot implementation work, use `java-spring-boot` first and `backend-docs-sync` second.
