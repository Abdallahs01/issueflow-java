# IssueFlow Implementation Instructions

These were the working instructions followed while building the project:

- Continue from the provided Java Spring Boot skeleton and do not rewrite the project from scratch.
- Use Java 21, Spring Boot, Maven wrapper, PostgreSQL through `compose.yml`, and H2 for tests.
- Keep the backend REST-focused; this project does not include a frontend UI.
- Implement the API contract from `README.md` and document exact run steps in `run.md`.
- Protect API endpoints with JWT authentication, while allowing `POST /users` and `POST /auth/login` for first-user bootstrap and login.
- Keep state-changing logic in services, keep controllers thin, and return DTOs instead of entities.
- Use validation annotations, enum constraints, and clear error responses for invalid input.
- Use soft delete for tickets and projects instead of permanent deletion.
- Record state-changing user and system actions in the audit log.
- Add focused tests for the key rules: ticket lifecycle, blockers, mentions, auth, soft delete, CSV import, escalation, and workload assignment.
- Document AI usage and relevant prompts in `prompts.md`.
