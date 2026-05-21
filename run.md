# IssueFlow Run Guide

## Requirements

- Java 21
- Docker Desktop
- PowerShell or another terminal

This project was developed and verified with Eclipse Temurin JDK 21.

## Start PostgreSQL

From the `issueflow-java` folder:

```powershell
docker compose up -d
```

The database configuration is:

```text
Host: localhost
Port: 5432
Database: issueflow
Username: issueflow
Password: issueflow
```

## Build And Test

```powershell
.\mvnw.cmd test
```

Expected result:

```text
BUILD SUCCESS
```

## Run The Application

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

This is a backend REST API, not a visual website. Most endpoints require a JWT token.

## Create First User

`POST http://localhost:8080/users`

```json
{
  "username": "admin",
  "email": "admin@example.com",
  "password": "password123",
  "fullName": "Admin User",
  "role": "ADMIN"
}
```

Use this user's `id` as `ownerId` when creating projects.

## Login

`POST http://localhost:8080/auth/login`

```json
{
  "username": "admin",
  "password": "password123"
}
```

The response contains:

```json
{
  "token": "...",
  "tokenType": "Bearer"
}
```

Use the token on protected endpoints:

```text
Authorization: Bearer <token>
```

## Implemented Features

- Users API
- JWT authentication
- Projects API
- Tickets API
- Comments API
- Audit logs
- Ticket dependencies/blockers
- Attachments
- CSV ticket import/export
- Soft delete and restore for projects/tickets
- ADMIN-only deleted/restore/delete controls for projects/tickets
- Mentions in comments
- Auto-escalation scheduler
- Auto-assignment to least-loaded developer
- Project workload endpoint

## Useful Endpoints

```text
POST   /users
GET    /users
GET    /users/{userId}
POST   /users/update/{userId}
DELETE /users/{userId}
GET    /users/{userId}/mentions

POST   /auth/login
POST   /auth/logout
GET    /auth/me

POST   /projects
GET    /projects
GET    /projects/{projectId}
PATCH  /projects/{projectId}
POST   /projects/update/{projectId}
DELETE /projects/{projectId}
GET    /projects/deleted
POST   /projects/{projectId}/restore
GET    /projects/{projectId}/workload

POST   /tickets
GET    /tickets?projectId={projectId}
GET    /tickets/{ticketId}
PATCH  /tickets/{ticketId}
POST   /tickets/update/{ticketId}
DELETE /tickets/{ticketId}
GET    /tickets/deleted?projectId={projectId}
POST   /tickets/{ticketId}/restore

POST   /tickets/{ticketId}/comments
GET    /tickets/{ticketId}/comments
PATCH  /tickets/{ticketId}/comments/{commentId}
POST   /tickets/{ticketId}/comments/update/{commentId}
DELETE /tickets/{ticketId}/comments/{commentId}

POST   /tickets/{ticketId}/dependencies
GET    /tickets/{ticketId}/dependencies
DELETE /tickets/{ticketId}/dependencies/{blockerId}

POST   /tickets/{ticketId}/attachments
GET    /tickets/{ticketId}/attachments
DELETE /tickets/{ticketId}/attachments/{attachmentId}

GET    /tickets/export?projectId={projectId}
POST   /tickets/import?projectId={projectId}

GET    /audit-logs
```

## Project Request Example

`POST http://localhost:8080/projects`

```json
{
  "name": "Sample Project",
  "description": "A sample project",
  "ownerId": 1
}
```

## Mentions Response

`GET /users/{userId}/mentions?page=1&pageSize=20` returns:

```json
{
  "data": [],
  "total": 0,
  "page": 1
}
```

## CSV Import Format

```csv
title,description,status,priority,type,assigneeId,dueDate
Imported ticket,Created from CSV,TODO,HIGH,BUG,,2026-06-01
```

## Notes

- Passwords are stored as BCrypt hashes, not plain text.
- Hibernate creates and updates the database schema from the JPA entities.
- The legacy starter `schema.sql` and `data.sql` files were removed so the application does not create unrelated sample tables.
