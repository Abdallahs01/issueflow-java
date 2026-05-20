# AI Usage Documentation

Model used:

```text
GPT-5 Codex
```

The project was built with AI assistance through an iterative development process. The main prompts and requests used were:

## Requirements Understanding

```text
Read the requirements PDF and explain carefully what needs to be done step by step.
```

## Spring Boot Structure

```text
Use Java Spring Boot. Explain IssueFlowApplication.java and then build a clean project structure with controllers, services, repositories, entities, DTOs, and common error handling.
```

## Authentication

```text
Implement JWT authentication with login, logout, and current-user endpoints. Store passwords securely and protect the API endpoints.
```

## Role-Based Authorization

```text
Implement ADMIN-only access for deleted and restore endpoints. Make audit logs record the authenticated user instead of always SYSTEM.
```

## Ticket Dependencies

```text
Implement ticket dependencies/blockers. A ticket cannot move to DONE while it has unresolved blockers. Both tickets must be in the same project.
```

## Attachments

```text
Implement ticket attachments with multipart upload, delete, metadata responses, max 10 MB size, and allowed content types: PNG, JPEG, PDF, and text.
```

## CSV Import And Export

```text
Implement CSV ticket export and import. Export tickets by project. Import tickets from CSV and return created, failed, and errors.
```

## Mentions

```text
Implement @username mentions in comments. Validate mentioned users, store mentions, return mentionedUsers in comment responses, and add GET /users/{userId}/mentions.
```

## Auto-Escalation

```text
Implement a background scheduler that escalates overdue tickets from LOW to MEDIUM to HIGH to CRITICAL, and marks CRITICAL overdue tickets as overdue.
```

## Auto-Assignment

```text
Implement auto-assignment when a ticket is created without an assignee. Assign to the least-loaded DEVELOPER in the project, break ties by oldest registered user, add workload endpoint, and audit AUTO_ASSIGN with actor SYSTEM.
```

## Cleanup And Documentation

```text
Fix unused variables/imports, keep the code clear, and create run.md and prompts.md for submission.
```

## Notes

- AI-generated code was reviewed during implementation.
- The project was compiled and checked with Maven after major changes.
- Live API checks were run for authentication, authorization, dependencies, attachments, CSV import/export, mentions, and soft-delete behavior.
