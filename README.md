# IssueFlow - Ticket Management Backend Platform

IssueFlow is a Spring Boot backend for a lightweight issue-tracking system. It manages users, projects, tickets, comments, audit logs, dependencies, attachments, CSV import/export, mentions, soft delete/restore, auto-escalation, and auto-assignment.

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Security with JWT
- Spring Data JPA / Hibernate
- PostgreSQL for local runtime
- H2 for automated tests
- Maven wrapper

Most endpoints require a JWT bearer token. `POST /users` and `POST /auth/login` are intentionally public so the first user can be created and then used to log in.

## Core Behavior

- Tickets and projects are soft-deleted, not permanently deleted.
- Deleted tickets/projects are hidden from normal list and get-by-id endpoints.
- Restore and deleted-list endpoints are ADMIN-only.
- Ticket status can only move forward: `TODO -> IN_PROGRESS -> IN_REVIEW -> DONE`.
- A `DONE` ticket cannot be edited.
- A ticket cannot move to `DONE` while it has unresolved blockers.
- Comments support case-insensitive `@username` mentions.
- Unassigned tickets are auto-assigned to the least-loaded DEVELOPER linked to the project.
- Project workload returns only developers linked to that project.
- If a project has no linked developers, an unassigned ticket remains unassigned.
- Overdue tickets are escalated by the scheduler at most once per UTC day.
- `CRITICAL` overdue tickets are marked as overdue instead of being escalated further.

## API Contract

### Users

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Create user | `POST /users` | `{ "username": "jdoe", "email": "jdoe@example.com", "password": "password123", "fullName": "John Doe", "role": "DEVELOPER" }` | `{ "id": 1, "username": "jdoe", "email": "jdoe@example.com", "fullName": "John Doe", "role": "DEVELOPER" }` |
| List users | `GET /users` | | `[ { "id": 1, "username": "jdoe", "email": "jdoe@example.com", "fullName": "John Doe", "role": "DEVELOPER" } ]` |
| Get user | `GET /users/{userId}` | | `{ "id": 1, "username": "jdoe", "email": "jdoe@example.com", "fullName": "John Doe", "role": "DEVELOPER" }` |
| Update user | `POST /users/update/{userId}` | `{ "fullName": "Jane Doe", "role": "ADMIN" }` | `200 OK` |
| Delete user | `DELETE /users/{userId}` | | `200 OK` |
| Get user mentions | `GET /users/{userId}/mentions?page=1&pageSize=20` | | `{ "data": [], "total": 0, "page": 1 }` |

### Authentication

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Login | `POST /auth/login` | `{ "username": "jdoe", "password": "password123" }` | `{ "token": "<jwt>", "tokenType": "Bearer" }` |
| Logout | `POST /auth/logout` | | `200 OK` |
| Current user | `GET /auth/me` | | `{ "id": 1, "username": "jdoe", "email": "jdoe@example.com", "fullName": "John Doe", "role": "DEVELOPER" }` |

### Projects

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Create project | `POST /projects` | `{ "name": "Sample Project", "description": "A sample project", "ownerId": 1, "developerIds": [2, 3] }` | `{ "id": 1, "name": "Sample Project", "description": "A sample project", "ownerId": 1, "developerIds": [2, 3], "deleted": false }` |
| List active projects | `GET /projects` | | `[ { "id": 1, "name": "Sample Project", "description": "A sample project", "ownerId": 1, "developerIds": [2, 3], "deleted": false } ]` |
| Get project | `GET /projects/{projectId}` | | `{ "id": 1, "name": "Sample Project", "description": "A sample project", "ownerId": 1, "developerIds": [2, 3], "deleted": false }` |
| Update project | `PATCH /projects/{projectId}` | `{ "name": "Updated Name", "description": "Updated description", "developerIds": [2, 3] }` | `200 OK` |
| Update project legacy route | `POST /projects/update/{projectId}` | `{ "name": "Updated Name", "description": "Updated description", "developerIds": [2, 3] }` | `200 OK` |
| Soft-delete project | `DELETE /projects/{projectId}` | | `200 OK` |
| List deleted projects | `GET /projects/deleted` | | `[ { "id": 1, "name": "Sample Project", "description": "A sample project", "ownerId": 1, "developerIds": [2, 3], "deleted": true } ]` |
| Restore project | `POST /projects/{projectId}/restore` | | `200 OK` |
| Project workload | `GET /projects/{projectId}/workload` | | `[ { "userId": 2, "username": "jdoe", "openTicketCount": 3 } ]` |

If the owner is a DEVELOPER, the owner is automatically linked to the project. Extra linked developers can be provided with `developerIds`.

### Tickets

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Create ticket | `POST /tickets` | `{ "title": "Fix login bug", "description": "...", "status": "TODO", "priority": "HIGH", "type": "BUG", "projectId": 1, "assigneeId": 2, "dueDate": "2026-06-01T00:00:00Z" }` | `{ "id": 1, "title": "Fix login bug", "description": "...", "status": "TODO", "priority": "HIGH", "type": "BUG", "projectId": 1, "assigneeId": 2, "dueDate": "2026-06-01T00:00:00Z", "overdue": false, "deleted": false, "lastAutoEscalatedAt": null, "version": 0 }` |
| List project tickets | `GET /tickets?projectId={projectId}` | | `[ { "id": 1, "title": "Fix login bug", "status": "TODO", "priority": "HIGH", "type": "BUG", "projectId": 1 } ]` |
| Get ticket | `GET /tickets/{ticketId}` | | `{ "id": 1, "title": "Fix login bug", "status": "TODO", "priority": "HIGH", "type": "BUG", "projectId": 1 }` |
| Update ticket | `PATCH /tickets/{ticketId}` | `{ "title": "...", "description": "...", "status": "IN_PROGRESS", "priority": "MEDIUM", "assigneeId": 3, "dueDate": "2026-06-01T00:00:00Z" }` | `200 OK` |
| Update ticket legacy route | `POST /tickets/update/{ticketId}` | `{ "status": "IN_PROGRESS" }` | `200 OK` |
| Soft-delete ticket | `DELETE /tickets/{ticketId}` | | `200 OK` |
| List deleted tickets | `GET /tickets/deleted?projectId={projectId}` | | `[ { "id": 1, "title": "Fix login bug", "deleted": true } ]` |
| Restore ticket | `POST /tickets/{ticketId}/restore` | | `200 OK` |
| Export tickets | `GET /tickets/export?projectId={projectId}` | | CSV with `id,title,description,status,priority,type,projectId,assigneeId,dueDate` |
| Import tickets | `POST /tickets/import?projectId={projectId}` | multipart `file` | `{ "created": 1, "failed": 0, "errors": [] }` |

### Comments And Mentions

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Add comment | `POST /tickets/{ticketId}/comments` | `{ "authorId": 2, "content": "Hello @jdoe!" }` | `{ "id": 1, "ticketId": 1, "authorId": 2, "content": "Hello @jdoe!", "mentionedUsers": [ { "id": 1, "username": "jdoe", "fullName": "John Doe" } ] }` |
| List comments | `GET /tickets/{ticketId}/comments` | | `[ { "id": 1, "ticketId": 1, "authorId": 2, "content": "Hello @jdoe!", "mentionedUsers": [] } ]` |
| Update comment | `PATCH /tickets/{ticketId}/comments/{commentId}` | `{ "content": "Updated comment" }` | `200 OK` |
| Update comment legacy route | `POST /tickets/{ticketId}/comments/update/{commentId}` | `{ "content": "Updated comment" }` | `200 OK` |
| Delete comment | `DELETE /tickets/{ticketId}/comments/{commentId}` | | `200 OK` |

### Dependencies

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Add dependency | `POST /tickets/{ticketId}/dependencies` | `{ "blockedBy": 42 }` | `200 OK` |
| List dependencies | `GET /tickets/{ticketId}/dependencies` | | `[ { "id": 42, "title": "Blocking ticket", "status": "IN_PROGRESS" } ]` |
| Remove dependency | `DELETE /tickets/{ticketId}/dependencies/{blockerId}` | | `200 OK` |

### Attachments

| Description | Endpoint | Request Body | Response |
|---|---|---|---|
| Upload attachment | `POST /tickets/{ticketId}/attachments` | multipart `file` | `{ "id": 1, "ticketId": 1, "filename": "screenshot.png", "contentType": "image/png", "sizeBytes": 1200 }` |
| List attachments | `GET /tickets/{ticketId}/attachments` | | `[ { "id": 1, "ticketId": 1, "filename": "screenshot.png", "contentType": "image/png", "sizeBytes": 1200 } ]` |
| Delete attachment | `DELETE /tickets/{ticketId}/attachments/{attachmentId}` | | `200 OK` |

Allowed attachment content types are PNG, JPEG, PDF, and plain text. The maximum upload size is 10 MB.

### Audit Logs

| Description | Endpoint | Query Params | Response |
|---|---|---|---|
| List audit logs | `GET /audit-logs` | Optional: `entityType`, `entityId`, `action`, `actor` | `[ { "id": 1, "action": "CREATE", "entityType": "TICKET", "entityId": 5, "actor": "SYSTEM", "details": "...", "createdAt": "2026-05-22T10:00:00Z" } ]` |

## Run

See [run.md](run.md) for exact setup, build, test, and Thunder Client usage steps.

## AI Documentation

See [prompts.md](prompts.md) for the main AI prompts and assistance used while building the project.
