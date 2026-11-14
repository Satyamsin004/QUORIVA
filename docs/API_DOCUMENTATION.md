# CS Scholar AI — API Documentation

## 1. Overview

CS Scholar AI provides RESTful APIs distributed across two primary services:
1. **Spring Boot Main Backend API**: Domain business logic, authentication, user workspaces, and document catalog management.
2. **FastAPI AI Service**: Specialized internal AI service for document chunking, vector embeddings, similarity search, and streaming RAG question answering.

---

## 2. Base URLs & Conventions

| Service | Environment | Base URL |
|---|---|---|
| Main Backend | Local | `http://localhost:8080/api/v1` |
| Main Backend Actuator | Local | `http://localhost:8080/actuator` |
| AI Service | Local (Internal) | `http://localhost:8000/api/v1` |
| AI Service Root Health | Local (Internal) | `http://localhost:8000/health` |
| AI Service Swagger UI | Local | `http://localhost:8000/docs` |

### Standards
- **Data Format**: `application/json` (UTF-8)
- **Authentication**: JWT Bearer token in HTTP header: `Authorization: Bearer <access_token>`, with dual support for `HttpOnly` refresh token cookies (`SameSite=Lax`, `Path=/api/v1/auth`).
- **Distributed Tracing**: `X-Correlation-ID` header returned on all responses and propagated to internal AI services.
- **Rate Limiting**: Throttled requests return HTTP 429 Too Many Requests with a `Retry-After: 60` header.
- **Internal Service Authentication**: Spring Boot calls to Python AI Service endpoints include `X-Internal-Service-Key`, `X-Internal-Timestamp`, and `X-Internal-Signature` (HMAC-SHA256).
- **Actuator Access Control**: `/actuator/health` and `/actuator/info` are public. All detailed metric endpoints require `ADMIN` role.
- **Response Wrapper**: Standard `ApiResponse<T>` envelope:
  ```json
  {
    "success": true,
    "message": "Operation successful",
    "data": { ... },
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```
- **Error Response**: RFC 7807-aligned `ErrorResponse`:
  ```json
  {
    "status": 400,
    "error": "Validation Error",
    "message": "Input validation failed. Please check the provided fields.",
    "errors": {
      "email": "Please provide a valid email address"
    },
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```
- **Rate Limit Response (429)**:
  ```json
  {
    "status": 429,
    "error": "Too Many Requests",
    "message": "Too many authentication attempts. Please try again after 60 seconds."
  }
  ```

---

## 3. Implemented API Endpoints (Phase 0, Phase 1 & Phase 2)

### 3.1 Authentication & User Management

#### `POST /api/v1/auth/register`
- **Description**: Registers a new student account, hashes the password, auto-provisions a default personal workspace, and returns access/refresh JWTs.
- **Authentication**: Public
- **Request Body**:
  ```json
  {
    "fullName": "Ada Lovelace",
    "email": "ada@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "User registered successfully.",
    "data": {
      "accessToken": "eyJhbGciOi...",
      "tokenType": "Bearer",
      "expiresInMs": 900000,
      "user": {
        "id": "e4b2d56a-1234-4567-89ab-cdef01234567",
        "fullName": "Ada Lovelace",
        "email": "ada@example.com",
        "role": "STUDENT",
        "profileImageUrl": null,
        "active": true,
        "createdAt": "2026-11-11T10:15:00Z",
        "updatedAt": "2026-11-11T10:15:00Z"
      }
    },
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```
  *Cookies Set:*
  - `refresh_token`: `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax` (Secure in production)
  - `XSRF-TOKEN`: `HttpOnly=false`, `Path=/`, `SameSite=Lax` (Double-Submit CSRF token)
- **Common Errors**: `400 Bad Request` (Validation errors), `409 Conflict` (Email already registered).

#### `POST /api/v1/auth/login`
- **Description**: Authenticates user via email and password, issuing access and refresh JWTs.
- **Authentication**: Public
- **Request Body**:
  ```json
  {
    "email": "ada@example.com",
    "password": "StrongPassword123!"
  }
  ```
- **Response** (`200 OK`): Same structure as `POST /api/v1/auth/register`.
- **Common Errors**: `401 Unauthorized` ("Invalid email or password.").

#### `POST /api/v1/auth/refresh` (Alias: `POST /api/v1/auth/refresh-token`)
- **Description**: Rotates refresh tokens and issues a new access token within the active token family. The refresh token is delivered and rotated exclusively via `HttpOnly` cookie (`refresh_token`). Cookie-based requests require the `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie (Double-Submit CSRF protection). Replaying an already-revoked refresh token triggers immediate revocation of all tokens in that device family. (Programmatic clients may optionally supply `refreshToken` in request body).
- **Authentication**: Cookie or Public Body
- **Request Headers**:
  - `X-XSRF-TOKEN`: `<csrf_token>` (required when authenticating via cookies)
- **Response** (`200 OK`): New `AuthResponse` containing `accessToken`, `tokenType`, `expiresInMs`, and `user`. The new rotated refresh token is set in the `Set-Cookie` header (`HttpOnly`).
- **Common Errors**: `401 Unauthorized` (Token expired or revoked), `403 Forbidden` (Missing or mismatched CSRF token).

#### `POST /api/v1/auth/logout`
- **Description**: Revokes the refresh token family in the database, invalidates user session, and clears `refresh_token` and `XSRF-TOKEN` cookies (`Max-Age=0`). Requires `X-XSRF-TOKEN` header if relying on cookie credentials.
- **Authentication**: Optional / Cookie / Authenticated
- **Request Headers**:
  - `X-XSRF-TOKEN`: `<csrf_token>` (when authenticating via cookies)
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Logged out successfully.",
    "data": null,
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```

#### `GET /api/v1/auth/me`
- **Description**: Retrieves current authenticated user's profile.
- **Authentication**: Bearer Token required
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Current user profile retrieved.",
    "data": {
      "id": "e4b2d56a-1234-4567-89ab-cdef01234567",
      "fullName": "Ada Lovelace",
      "email": "ada@example.com",
      "role": "STUDENT",
      "profileImageUrl": null,
      "active": true,
      "createdAt": "2026-11-11T10:15:00Z",
      "updatedAt": "2026-11-11T10:15:00Z"
    },
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```
- **Common Errors**: `401 Unauthorized`.

#### `PATCH /api/v1/users/me`
- **Description**: Updates profile information (fullName and profileImageUrl). Role and email cannot be altered.
- **Authentication**: Bearer Token required
- **Request Body**:
  ```json
  {
    "fullName": "Ada Lovelace, FRS",
    "profileImageUrl": "https://example.com/avatar.jpg"
  }
  ```
- **Response** (`200 OK`): Updated `UserResponse`.
- **Common Errors**: `400 Bad Request`, `401 Unauthorized`.

---

### 3.2 Workspace Management

#### `POST /api/v1/workspaces`
- **Description**: Creates an academic study workspace. The authenticated creator becomes the workspace OWNER.
- **Authentication**: Bearer Token required
- **Request Body**:
  ```json
  {
    "name": "CS189 - Introduction to Machine Learning",
    "description": "Fall 2026 Course Materials, Lecture Notes, and Problem Sets"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "Workspace created successfully.",
    "data": {
      "id": "b3f12a80-1234-4567-89ab-cdef01234567",
      "name": "CS189 - Introduction to Machine Learning",
      "description": "Fall 2026 Course Materials, Lecture Notes, and Problem Sets",
      "ownerId": "e4b2d56a-1234-4567-89ab-cdef01234567",
      "ownerName": "Ada Lovelace",
      "createdAt": "2026-11-11T10:15:00Z",
      "updatedAt": "2026-11-11T10:15:00Z"
    },
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```

#### `GET /api/v1/workspaces`
- **Description**: Lists all workspaces accessible by the authenticated user (owned or member).
- **Authentication**: Bearer Token required
- **Response** (`200 OK`): Array of `WorkspaceResponse` objects.

#### `GET /api/v1/workspaces/{id}`
- **Description**: Retrieves a single workspace by ID. Access is strictly validated to ensure only the owner or authorized members can view.
- **Authentication**: Bearer Token required
- **Response** (`200 OK`): `WorkspaceResponse`.
- **Common Errors**: `403 Forbidden` (User does not belong to workspace), `404 Not Found`.

#### `PATCH /api/v1/workspaces/{id}`
- **Description**: Updates workspace name and/or description.
- **Authentication**: Bearer Token required (Owner or authorized member)
- **Request Body**:
  ```json
  {
    "name": "CS189 - Machine Learning (Honors)",
    "description": "Updated syllabus"
  }
  ```
- **Response** (`200 OK`): Updated `WorkspaceResponse`.

#### `DELETE /api/v1/workspaces/{id}`
- **Description**: Permanently deletes a workspace. Only the workspace OWNER can execute deletion.
- **Authentication**: Bearer Token required (Owner only)
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Workspace deleted successfully.",
    "data": null,
    "timestamp": "2026-11-11T10:15:00Z"
  }
  ```
- **Common Errors**: `403 Forbidden` (User is not the owner), `404 Not Found`.

---

### 3.3 Document Management & S3/MinIO Storage

#### Direct-to-Storage Upload Flow

```text
Student Browser                 Spring Boot API                    MinIO / S3 Storage
       |                              |                                    |
       | 1. POST /upload-url          |                                    |
       |----------------------------->|                                    |
       |                              | (Validates workspace, file specs,  |
       |                              |  creates PENDING_UPLOAD record,   |
       |                              |  generates presigned PUT URL)      |
       | 2. Presigned URL + storageKey|                                    |
       |<-----------------------------|                                    |
       |                                                                   |
       | 3. Direct HTTP PUT with file bytes                                |
       |------------------------------------------------------------------>|
       |                                                                   |
       | 4. POST /{id}/complete-upload|                                    |
       |----------------------------->|                                    |
       |                              | (Checks object existence,          |
       |                              |  validates size & magic bytes,     |
       |                              |  verifies SHA-256 hash,            |
       |                              |  transitions status to UPLOADED)   |
       | 5. Verified DocumentResponse |                                    |
       |<-----------------------------|                                    |
```

#### `POST /api/v1/documents/upload-url`
- **Description**: Validates file metadata, verifies workspace membership, provisions a `PENDING_UPLOAD` document record, and returns a short-lived presigned S3 upload URL.
- **Authentication**: Bearer Token (Requires OWNER or MEMBER role in workspace)
- **Request Body**:
  ```json
  {
    "workspaceId": "d4417878-5533-4c4c-acf7-044e5026ec20",
    "filename": "operating_systems_notes.pdf",
    "contentType": "application/pdf",
    "fileSize": 5242880,
    "checksumSha256": "optional-sha256-hex-digest",
    "title": "Operating Systems - Chapter 1",
    "description": "Lecture notes covering process scheduling"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "Presigned upload URL generated successfully.",
    "data": {
      "documentId": "e20c0851-27cf-4ef3-aa76-68a255893c59",
      "uploadUrl": "http://localhost:9000/cs-scholar-documents/workspaces/.../operating_systems_notes.pdf?X-Amz-...",
      "storageKey": "workspaces/d4417878-5533-4c4c-acf7-044e5026ec20/documents/e20c0851-27cf-4ef3-aa76-68a255893c59/operating_systems_notes.pdf",
      "expiresInSeconds": 900,
      "requiredHeaders": {
        "Content-Type": "application/pdf",
        "Content-Length": "5242880"
      }
    },
    "timestamp": "2026-11-11T10:50:00Z"
  }
  ```
- **Common Errors**: `400 Bad Request` (Invalid format, oversized file, path traversal characters), `403 Forbidden` (User lacks workspace upload permissions), `404 Not Found` (Workspace not found).

#### `POST /api/v1/documents/{id}/complete-upload`
- **Description**: Verifies that the file was successfully written to S3/MinIO, inspects actual file size, checks magic bytes (`%PDF-` or PK zip for DOCX/PPTX), verifies SHA-256 integrity, transitions status to `UPLOADED`, and records `uploadedAt`. Operation is idempotent.
- **Authentication**: Bearer Token
- **Request Body** (Optional):
  ```json
  {
    "checksumSha256": "optional-client-sha256"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Document upload verified and completed successfully.",
    "data": {
      "id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
      "workspaceId": "d4417878-5533-4c4c-acf7-044e5026ec20",
      "workspaceName": "Operating Systems",
      "ownerId": "55f3d6ce-928d-4f59-881c-d2a6ba985ce2",
      "ownerName": "Ada Lovelace",
      "title": "Operating Systems - Chapter 1",
      "originalFilename": "operating_systems_notes.pdf",
      "storageKey": "workspaces/d4417878-5533-4c4c-acf7-044e5026ec20/documents/e20c0851-27cf-4ef3-aa76-68a255893c59/operating_systems_notes.pdf",
      "fileType": "PDF",
      "mimeType": "application/pdf",
      "fileSize": 5242880,
      "checksumSha256": "4a7db5a2c4e...",
      "pageCount": null,
      "description": "Lecture notes covering process scheduling",
      "processingStatus": "UPLOADED",
      "visibility": "PRIVATE",
      "createdAt": "2026-11-11T10:50:00Z",
      "updatedAt": "2026-11-11T10:52:00Z",
      "uploadedAt": "2026-11-11T10:52:00Z"
    },
    "timestamp": "2026-11-11T10:52:00Z"
  }
  ```
- **Common Errors**: `400 Bad Request` (Object missing from storage, magic byte check failed, checksum mismatch), `403 Forbidden` (Not workspace member), `404 Not Found`.

#### `GET /api/v1/documents`
- **Description**: Lists documents with workspace-level authorization filtering, search, pagination, and sorting.
- **Authentication**: Bearer Token
- **Query Parameters**:
  - `workspaceId` (UUID, optional): Filter by specific workspace.
  - `query` (String, optional): Case-insensitive search across titles.
  - `fileType` (String, optional): `PDF`, `DOCX`, `PPTX`.
  - `status` (String, optional): `PENDING_UPLOAD`, `UPLOADED`, etc.
  - `page` (int, default `0`): Zero-based page index.
  - `size` (int, default `20`, max `100`): Page size.
  - `sort` (String, default `createdAt,desc`): Property and direction.
- **Response** (`200 OK`): Paginated `Page<DocumentResponse>`.

#### `GET /api/v1/documents/{id}`
- **Description**: Retrieves document metadata for an authorized workspace member.
- **Authentication**: Bearer Token
- **Response** (`200 OK`): Returns `DocumentResponse`.
- **Common Errors**: `403 Forbidden`, `404 Not Found` (Document not found or soft-deleted).

#### `GET /api/v1/documents/{id}/download-url`
- **Description**: Generates a short-lived presigned GET URL for direct download from MinIO/S3. Only available for documents in `UPLOADED` or processed states.
- **Authentication**: Bearer Token
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Presigned download URL generated successfully.",
    "data": {
      "documentId": "e20c0851-27cf-4ef3-aa76-68a255893c59",
      "downloadUrl": "http://localhost:9000/cs-scholar-documents/workspaces/.../operating_systems_notes.pdf?response-content-disposition=attachment...",
      "expiresInSeconds": 900
    },
    "timestamp": "2026-11-11T10:55:00Z"
  }
  ```
- **Common Errors**: `400 Bad Request` (Document still in `PENDING_UPLOAD` or `DELETED`), `403 Forbidden`, `404 Not Found`.

#### `GET /api/v1/documents/{id}/status`
- **Description**: Returns upload, processing, and vector embedding lifecycle status for a document.
- **Authentication**: Bearer Token
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Document status retrieved successfully.",
    "data": {
      "documentId": "e20c0851-27cf-4ef3-aa76-68a255893c59",
      "processingStatus": "PROCESSED",
      "embeddingStatus": "EMBEDDED",
      "pageCount": 42,
      "errorMessage": null,
      "uploadedAt": "2026-11-11T10:52:00Z",
      "updatedAt": "2026-11-11T10:52:00Z"
    },
    "timestamp": "2026-11-11T10:55:00Z"
  }
  ```

#### `DELETE /api/v1/documents/{id}`
- **Description**: Safely soft-deletes a document record and removes the underlying object from MinIO/S3 storage. Permitted only for the document owner, workspace owner, or workspace member with editor permissions.
- **Authentication**: Bearer Token
- **Response** (`200 OK`): Standard success response with `data: null`.
- **Common Errors**: `403 Forbidden` (Viewer role or unauthorized user), `404 Not Found`.

### 3.4 Health & Diagnostics

#### `GET /actuator/health`
- **Description**: Spring Boot Actuator health probe reporting PostgreSQL, Redis, and internal AI service availability.
- **Authentication**: Public
- **Response** (`200 OK` or `503 Service Unavailable`):
  ```json
  {
    "status": "UP",
    "components": {
      "db": { "status": "UP" },
      "redis": { "status": "UP" },
      "aiService": { "status": "UP" }
    }
  }
  ```

#### `GET /health` (AI Service)
- **Description**: Internal FastAPI health check.
- **Authentication**: Public (Internal network)

### 3.5 Internal Ingestion Engine API (AI Service)

These endpoints are strictly for inter-service communication between Spring Boot and the FastAPI AI service, authenticated using the shared `X-Internal-Service-Key` header. They are not exposed to the public internet or the frontend.

#### `POST /internal/v1/ingestion/process`
- **Description**: Triggers asynchronous document ingestion, structure-aware chunking, and database persistence. Initializes a `ProcessingJob` record and enqueues a Celery task.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Request Body**:
  ```json
  {
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "workspace_id": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "storage_key": "workspaces/f51d1962-38de-5fa4-bb87-79b366904d60/documents/e20c0851-27cf-4ef3-aa76-68a255893c59/relational_algebra.pdf",
    "file_type": "PDF",
    "mime_type": "application/pdf",
    "file_size": 154238,
    "processing_version": 1
  }
  ```
- **Response** (`202 Accepted`):
  ```json
  {
    "job_id": "7f1c015b-9a82-4168-98e7-c793f0bce421",
    "task_id": "0d8fa093-68d7-4003-8ea9-ea1e2b291a18",
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "status": "QUEUED",
    "message": "Document ingestion successfully queued for background processing."
  }
  ```
- **Common Errors**: `401 Unauthorized` (Missing header), `403 Forbidden` (Invalid key), `404 Not Found` (Document does not exist).

#### `GET /internal/v1/ingestion/jobs/{job_id}`
- **Description**: Inspects processing job status, attempts count, error messages, and execution timestamps.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Response** (`200 OK`):
  ```json
  {
    "job_id": "7f1c015b-9a82-4168-98e7-c793f0bce421",
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "status": "COMPLETED",
    "attempts": 1,
    "max_attempts": 3,
    "error_message": null,
    "task_id": "0d8fa093-68d7-4003-8ea9-ea1e2b291a18",
    "processing_version": 1,
    "started_at": "2026-11-11T10:52:05Z",
    "completed_at": "2026-11-11T10:52:08Z",
    "created_at": "2026-11-11T10:52:04Z",
    "updated_at": "2026-11-11T10:52:08Z"
  }
  ```

### 3.4 Internal Embedding API (FastAPI — Phase 4 Implemented)

#### `POST /internal/v1/embeddings/process`
- **Description**: Triggers asynchronous vector embedding generation for a document's chunks using `BAAI/bge-base-en-v1.5`. Initializes an `EmbeddingJob` and enqueues a Celery worker task (`generate_document_embeddings`).
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Request Body**:
  ```json
  {
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59"
  }
  ```
- **Response** (`202 Accepted`):
  ```json
  {
    "job_id": "9a3d4ef2-1234-4567-89ab-cdef01234567",
    "task_id": "7b2c015b-9a82-4168-98e7-c793f0bce421",
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "status": "PENDING",
    "message": "Document embedding job successfully queued."
  }
  ```

#### `GET /internal/v1/embeddings/jobs/{job_id}`
- **Description**: Retrieves the status, chunk counts, segment counts, and error details of an embedding generation job.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Response** (`200 OK`):
  ```json
  {
    "job_id": "9a3d4ef2-1234-4567-89ab-cdef01234567",
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "status": "COMPLETED",
    "model_name": "BAAI/bge-base-en-v1.5",
    "model_version": "bge-base-en-v1.5",
    "total_chunks": 12,
    "processed_chunks": 12,
    "total_segments": 14,
    "attempts": 1,
    "max_attempts": 3,
    "error_message": null,
    "task_id": "7b2c015b-9a82-4168-98e7-c793f0bce421",
    "started_at": "2026-11-11T11:01:00Z",
    "completed_at": "2026-11-11T11:01:05Z",
    "created_at": "2026-11-11T11:00:59Z",
    "updated_at": "2026-11-11T11:01:05Z"
  }
  ```

#### `POST /internal/v1/embeddings/retry/{job_id}`
- **Description**: Retries a failed or stalled embedding job by re-queuing the Celery embedding task.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Response** (`202 Accepted`):
  ```json
  {
    "job_id": "9a3d4ef2-1234-4567-89ab-cdef01234567",
    "task_id": "8c3d015b-9a82-4168-98e7-c793f0bce422",
    "document_id": "e20c0851-27cf-4ef3-aa76-68a255893c59",
    "status": "PENDING",
    "message": "Embedding job retry successfully queued."
  }
  ```

### 3.5 Public Search API (Spring Boot — Phase 5 Implemented)

#### `POST /api/v1/search`
- **Description**: Executes authenticated, authorized hybrid search (dense pgvector semantic similarity + PostgreSQL full-text keyword matching + RRF + Cross-Encoder reranking) across documents in a target workspace.
- **Authentication**: `Authorization: Bearer <jwt_access_token>`
- **Request Body**:
  ```json
  {
    "query": "Explain binary search trees and their time complexity.",
    "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "topK": 10
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Search completed successfully.",
    "data": {
      "query": "Explain binary search trees and their time complexity.",
      "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
      "totalCandidates": 18,
      "results": [
        {
          "chunkId": "1a2b3c4d-0000-0000-0000-000000000001",
          "documentId": "e20c0851-27cf-4ef3-aa76-68a255893c59",
          "documentName": "Introduction_to_Algorithms.pdf",
          "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
          "chunkContent": "A binary search tree satisfies the binary-search-tree property...",
          "pageNumber": 287,
          "slideNumber": null,
          "sectionHeading": "12.1 What is a binary search tree?",
          "chunkIndex": 14,
          "sourceType": "PDF",
          "finalRank": 1,
          "rerankerScore": 0.9412,
          "rrfScore": 0.0325,
          "denseRank": 1,
          "keywordRank": 2,
          "denseSimilarity": 0.8924,
          "keywordScore": 0.4851,
          "segmentIndex": 0
        }
      ]
    },
    "timestamp": "2026-11-12T10:00:00Z"
  }
  ```
- **Common Errors**:
  - `400 Bad Request`: Query is blank, whitespace-only, or `topK` is out of bounds (1–100).
  - `401 Unauthorized`: Missing or invalid JWT access token.
  - `403 Forbidden`: Authenticated user does not have access to the target workspace.
  - `404 Not Found`: Target workspace does not exist.
  - `503 Service Unavailable`: AI retrieval engine unavailable or encountered internal inference errors.

### 3.6 Internal Hybrid Retrieval API (FastAPI — Phase 5 Implemented)

#### `POST /internal/v1/retrieval/search`
- **Description**: Internal endpoint called exclusively by Spring Boot with verified user and workspace scope parameters. Executes dense retrieval, PostgreSQL full-text search, RRF fusion, and cross-encoder reranking.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Request Body**:
  ```json
  {
    "query": "Explain binary search trees and their time complexity.",
    "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "userId": "e4b2d56a-1234-4567-89ab-cdef01234567",
    "topK": 10
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "query": "Explain binary search trees and their time complexity.",
    "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "totalCandidates": 18,
    "results": [ ... ]
  }
  ```

### 3.7 Academic Tutor & Conversations API (Spring Boot — Phase 6 Implemented)

#### `POST /api/v1/conversations`
- **Description**: Creates a new discussion thread for the authenticated student within an authorized workspace.
- **Authentication**: Bearer Token required
- **Request Body**:
  ```json
  {
    "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "title": "Virtual Memory and Paging"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "Conversation created successfully.",
    "data": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "workspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
      "title": "Virtual Memory and Paging",
      "status": "ACTIVE",
      "createdAt": "2026-11-12T10:45:00Z",
      "updatedAt": "2026-11-12T10:45:00Z",
      "lastMessageAt": "2026-11-12T10:45:00Z",
      "messageCount": 0
    },
    "timestamp": "2026-11-12T10:45:00Z"
  }
  ```

#### `GET /api/v1/conversations?workspaceId={uuid}&page=0&size=20`
- **Description**: Lists conversations for the authenticated student within a workspace.
- **Authentication**: Bearer Token required
- **Response** (`200 OK`): Paginated list of `ConversationResponse` objects.

#### `GET /api/v1/conversations/{id}`
- **Description**: Retrieves single conversation metadata.
- **Authentication**: Bearer Token required (Owner only)

#### `DELETE /api/v1/conversations/{id}`
- **Description**: Deletes a conversation and its messages.
- **Authentication**: Bearer Token required (Owner only)

#### `GET /api/v1/conversations/{id}/messages`
- **Description**: Fetches message history with verified source citations for an active discussion.
- **Authentication**: Bearer Token required
- **Response** (`200 OK`): Array of `MessageResponse` objects with `sources`.

#### `POST /api/v1/conversations/{id}/messages`
- **Description**: Sends student question, triggers grounded RAG pipeline, persists prompt & response, and returns assistant answer with page citations.
- **Authentication**: Bearer Token required
- **Request Body**:
  ```json
  {
    "content": "Explain FIFO page replacement and its Belady anomaly.",
    "provider": "groq",
    "modelName": "llama-3.3-70b-versatile"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Message sent and answer generated successfully.",
    "data": {
      "id": "m9a8b7c6-d5e4-3210-fedc-ba9876543210",
      "conversationId": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "role": "ASSISTANT",
      "content": "FIFO page replacement replaces the page that has been in memory longest [S1]. Belady's anomaly occurs when increasing page frames increases page faults [S2].",
      "status": "GROUNDED",
      "provider": "groq",
      "modelName": "llama-3.3-70b-versatile",
      "inputTokens": 450,
      "outputTokens": 95,
      "latencyMs": 310,
      "createdAt": "2026-11-12T10:46:00Z",
      "sources": [
        {
          "id": "s1a2b3c4-d5e6-7890-abcd-ef1234567890",
          "chunkId": "b1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "documentTitle": "Operating Systems Principles.pdf",
          "sourceRank": 1,
          "citationLabel": "S1",
          "relevanceScore": 0.945,
          "pageNumber": 312,
          "sectionHeading": "FIFO Page Replacement",
          "chunkContent": "FIFO associates each page with the time it was brought into memory..."
        }
      ]
    },
    "timestamp": "2026-11-12T10:46:00Z"
  }
  ```

### 3.8 Internal Academic Tutor RAG API (FastAPI — Phase 6 Implemented)

#### `POST /internal/v1/tutor/answer`
- **Description**: Internal endpoint called exclusively by Spring Boot to execute the grounded RAG pipeline.
- **Authentication**: `X-Internal-Service-Key: <secret>`
- **Request Body**:
  ```json
  {
    "query": "Explain FIFO page replacement.",
    "workspace_id": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "user_id": "e4b2d56a-1234-4567-89ab-cdef01234567",
    "conversation_id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
    "history": [
      {"role": "user", "content": "What is paging?"},
      {"role": "assistant", "content": "Paging is a memory management scheme."}
    ],
    "provider": "groq",
    "model_name": "llama-3.3-70b-versatile"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "answer": "FIFO page replacement algorithm replaces the oldest page in memory [S1].",
    "grounding_status": "GROUNDED",
    "insufficient_evidence": false,
    "citations": [ ... ],
    "provider": "groq",
    "model_name": "llama-3.3-70b-versatile",
    "input_tokens": 450,
    "output_tokens": 95,
    "latency_ms": 310
  }
  ```

---

## 3.9 Shared Academic Library (Phase 8)

### 3.9.1 Public / Discoverable Resources
#### `GET /api/v1/library/resources`
- **Description**: Search and browse published academic resources. Unapproved drafts, pending review items, and suspended resources are excluded.
- **Authentication**: Bearer Token
- **Query Parameters**:
  - `q` (optional): Full-text keyword query
  - `category` (optional): Academic category filter (e.g. `DATA_STRUCTURES`, `OPERATING_SYSTEMS`)
  - `resourceType` (optional): Resource type filter (e.g. `LECTURE_NOTES`, `TEXTBOOK`, `LAB_MANUAL`)
  - `academicLevel` (optional): Target academic level
  - `tag` (optional): Keyword tag filter
  - `sort`: `RECENT` (default), `POPULAR` (downloads count), `TITLE`
  - `page`, `size`: Pagination controls

#### `GET /api/v1/library/resources/{id}`
- **Description**: Get full resource details. Access is granted to any user for `PUBLISHED` resources; for unpublished resources, access is restricted to the contributor or an authorized moderator (`ADMIN` or `EDUCATOR`). Storage keys are never exposed in responses.

#### `GET /api/v1/library/resources/{id}/download`
- **Description**: Generates an expiring presigned S3 download URL and increments the resource download counter.

#### Taxonomy Metadata Endpoints:
- `GET /api/v1/library/categories`: List of academic categories with display labels.
- `GET /api/v1/library/resource-types`: List of resource types with display labels.
- `GET /api/v1/library/rights-types`: List of rights declaration classifications.
- `GET /api/v1/library/academic-levels`: Academic level options.
- `GET /api/v1/library/tags/popular`: Top most frequently tagged keywords.

### 3.9.2 Contributor Submission Lifecycle
#### `POST /api/v1/library/submissions/upload-url`
- **Description**: Generates a dedicated S3 upload slot prefixed with `library/{resourceId}/{filename}` ensuring complete physical and logical isolation from private workspace documents.

#### `POST /api/v1/library/submissions`
- **Description**: Creates a new resource record. Mandatory rights declaration, non-blank license specification, explicit legal warranty (`warrantyAgreed: true`), and verified S3 file presence are strictly enforced. Classification `OTHER` requires non-blank source attribution. Contributor can save as `DRAFT` or submit immediately for editorial review (`PENDING_REVIEW`).

#### `PUT /api/v1/library/submissions/{id}`
- **Description**: Updates metadata, tags, or licensing for a `DRAFT` or `REJECTED` submission. Non-owners are denied (HTTP 403).

#### `POST /api/v1/library/submissions/{id}/submit`
- **Description**: Submits a draft or revised submission for editorial review (`PENDING_REVIEW`). Re-verifies file presence in storage and blocks duplicate submissions.

#### `POST /api/v1/library/submissions/{id}/withdraw`
- **Description**: Withdraws a `PENDING_REVIEW` submission back to `DRAFT`.

#### `DELETE /api/v1/library/submissions/{id}`
- **Description**: Deletes a `DRAFT` or `REJECTED` submission. If the submission has associated moderation logs, it safely transitions to `REMOVED` status to preserve database-level audit immutability.

#### `GET /api/v1/library/my-submissions`
- **Description**: Retrieves all submissions authored by the current user across all active status states (excluding `REMOVED`).

### 3.9.3 Editorial Moderation Workflow (Educators & Admins Only)
Protected by `@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")`. `STUDENT` and `RESEARCHER` roles are strictly forbidden (HTTP 403).

#### `GET /api/v1/library/moderation/queue`
- **Description**: Lists all submissions currently awaiting review (`PENDING_REVIEW`).

#### `POST /api/v1/library/moderation/resources/{id}/approve`
- **Description**: Approves a submission $\to$ transitions status to `PUBLISHED`, sets `published_at`, and records moderation audit entry.

#### `POST /api/v1/library/moderation/resources/{id}/reject`
- **Description**: Rejects a submission $\to$ requires detailed feedback reason, transitions status to `REJECTED`, and records audit entry.

#### `POST /api/v1/library/moderation/resources/{id}/suspend`
- **Description**: Suspends an active published resource pending investigation $\to$ status becomes `SUSPENDED`.

#### `POST /api/v1/library/moderation/resources/{id}/restore`
- **Description**: Restores a suspended resource back to `PUBLISHED`.

#### `GET /api/v1/library/moderation/resources/{id}/logs` & `GET /api/v1/library/moderation/logs`
- **Description**: Retrieves immutable moderation audit history.

### 3.9.4 User Reporting
#### `POST /api/v1/library/resources/{id}/reports`
- **Description**: Any authenticated user can report a resource for copyright infringement, inappropriate content, incorrect attribution, corrupted files, or spam.

#### `GET /api/v1/library/moderation/reports` & `POST /api/v1/library/moderation/reports/{reportId}/resolve`
- **Description**: Moderators review and resolve or dismiss filed reports with resolution notes.

---

### 3.10 CS Topic Discovery & Knowledge Graph API (Phases 9 & 9.1 Implemented)

#### `GET /api/v1/knowledge/subjects`
- **Description**: Lists all 13 core CS academic subjects ordered by `display_order`, with real-time topic count per subject:
  - `CS-FND`: Programming Fundamentals
  - `CS-DSA`: Data Structures and Algorithms
  - `CS-OS`: Operating Systems
  - `CS-NET`: Computer Networks
  - `CS-DB`: Database Systems
  - `CS-SE`: Software Engineering
  - `CS-AI`: Artificial Intelligence & Machine Learning
  - `CS-SYS`: Distributed Systems & Cloud Computing
  - `CS-ARCH`: Computer Organization & Architecture
  - `CS-COMP`: Compiler Design
  - `CS-TOC`: Theory of Computation
  - `CS-SEC`: Cybersecurity & Information Assurance
  - `CS-WEB`: Web Development & Distributed Applications
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/subjects/{id}` & `GET /api/v1/knowledge/subjects/slug/{slug}`
- **Description**: Retrieves single subject metadata and description by UUID or URL slug (e.g. `data-structures-and-algorithms`, `compiler-design`).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/subjects/{subjectId}/topics`
- **Description**: Retrieves all topics categorized under the specified subject.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/topics`
- **Description**: Paginated search and discovery for knowledge topics across all 65 curated topics. Supports query `q`, `subjectId`, `difficulty` (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`), `topicType`, `page`, and `size`.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/topics/{idOrSlug}`
- **Description**: Retrieves detailed topic knowledge package:
  - Executive summary and comprehensive architectural/theoretical description.
  - Granular learning objectives.
  - Curated academic tags.
  - Prerequisites (concepts required before studying this topic).
  - Unlocked concepts (advanced topics enabled by mastering this one).
  - Related topics (cross-disciplinary concepts).
  - Subtopics (child concepts).
  - Linked published Shared Academic Library resources (dynamically filtered to `PUBLISHED` status only).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/graph`
- **Description**: Retrieves full knowledge graph slice with nodes and directed edges. Supports filtering by `subjectId` and bounded by `maxNodes` (default 50).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/graph/topics/{idOrSlug}`
- **Description**: Retrieves bounded neighborhood subgraph centered around a target topic up to specified `depth` (1..3 hops). Flags center node with `isCenter: true`.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/topics/{idOrSlug}/learning-path`
- **Description**: Generates a deterministic, cycle-safe topological prerequisite learning path (Kahn's algorithm) starting from foundations up to the target topic. Guarantees target topic is terminal step and aggregates total study minutes. If an edge cycle is encountered, returns `400 Bad Request` with `KnowledgeGraphIntegrityException` error payload detailing the cycle anomaly.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/knowledge/learning-paths`
- **Description**: Retrieves curated milestone learning tracks, including Full-Stack Web Architecture, Machine Learning Foundations, Cloud Systems, Instruction Pipelining (`CS-ARCH`), Compiler Optimizations (`CS-COMP`), Computational Complexity & NP-Completeness (`CS-TOC`), Network Security & Firewalls (`CS-SEC`), and SSR Web Performance (`CS-WEB`).
- **Authentication**: `Bearer <access_token>`

---

### 3.10 Interactive Learning Studio APIs (Spring Boot — Phase 10 Implemented)

#### `POST /api/v1/learning/quizzes/generate`
- **Description**: Generates an active practice quiz from workspace documents, CS knowledge graph topics, or published library resources.
- **Authentication**: `Bearer <access_token>`
- **Request Body**:
  ```json
  {
    "title": "CPU Scheduling Practice Quiz",
    "sourceType": "WORKSPACE_DOCUMENTS",
    "sourceWorkspaceId": "f51d1962-38de-5fa4-bb87-79b366904d60",
    "sourceDocumentIds": ["e20c0851-27cf-4ef3-aa76-68a255893c59"],
    "difficultyLevel": "MEDIUM",
    "questionCount": 5,
    "conceptFocus": "Round Robin and SJF"
  }
  ```
- **Response** (`201 Created`): `QuizSummaryDto`

#### `GET /api/v1/learning/quizzes`
- **Description**: Retrieves all quizzes generated by the authenticated user with question count and best score metrics.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/quizzes/{id}/practice`
- **Description**: Retrieves the client-safe practice projection for taking a quiz.
- **Security Invariant**: Strictly omits `isCorrect` from options and `explanation` from questions to prevent client-side answer key inspection.
- **Authentication**: `Bearer <access_token>`

#### `DELETE /api/v1/learning/quizzes/{id}`
- **Description**: Deletes a quiz and all associated questions and attempts.
- **Authentication**: `Bearer <access_token>`

#### `POST /api/v1/learning/quizzes/{id}/attempts`
- **Description**: Initiates a new practice attempt for the quiz, initializing status to `IN_PROGRESS`.
- **Authentication**: `Bearer <access_token>`

#### `PUT /api/v1/learning/attempts/{attemptId}/answers`
- **Description**: Saves intermediate student answer choices and review flags without grading.
- **Authentication**: `Bearer <access_token>`

#### `POST /api/v1/learning/attempts/{attemptId}/submit`
- **Description**: Submits the attempt for server-side evaluation. Calculates correct answers count, percentage score, locks the attempt into `SUBMITTED`, and rejects duplicate submissions (`400 Bad Request`).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/attempts/{attemptId}/review`
- **Description**: Post-submission attempt review. Reveals student selections, correct answer keys, and detailed academic explanations.
- **Authentication**: `Bearer <access_token>`

#### `POST /api/v1/learning/decks/manual` & `POST /api/v1/learning/decks/generate`
- **Description**: Creates flashcard decks either manually or through AI generation from academic contexts.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/decks` & `GET /api/v1/learning/decks/{id}`
- **Description**: Lists user's decks with due card counts or inspects single deck metadata.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/decks/{id}/cards` & `POST /api/v1/learning/decks/{id}/cards`
- **Description**: Manages cards within a specific deck (listing cards, adding new cards with front, back, and source citations).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/reviews/due`
- **Description**: Queries all cards currently due for SuperMemo-2 active recall review (`next_due_at <= now`). Supports optional `deckId` filter.
- **Authentication**: `Bearer <access_token>`

#### `POST /api/v1/learning/reviews`
- **Description**: Records review feedback (`AGAIN`, `HARD`, `GOOD`, `EASY`) and updates SM-2 repetition counts, ease factor ($\ge 1.30$), and next due timestamps.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/analytics/overview`
- **Description**: Returns aggregate learning metrics without fabrication: total attempts, average quiz score, cards due today, cards reviewed, and mastery state breakdown (`NEW`, `LEARNING`, `REVIEW`, `GRADUATED`).
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/analytics/topics`
- **Description**: Computes topic-level accuracy and mastery across quizzes and flashcards.
- **Authentication**: `Bearer <access_token>`

#### `GET /api/v1/learning/analytics/activity`
- **Description**: Returns daily activity timeline (quizzes taken, cards reviewed, study minutes) over requested day range (default 14 days).
- **Authentication**: `Bearer <access_token>`

---

### 3.11 Internal Learning Generation API (FastAPI — Phase 10 Implemented)

#### `POST /internal/v1/learning/quizzes/generate`
- **Description**: Generates structured MCQ and True/False questions with distractor verification and single-correct-answer invariants.
- **Authentication**: `X-Internal-Service-Key: <secret>`

#### `POST /internal/v1/learning/flashcards/generate`
- **Description**: Generates active recall flashcards with question prompts, concise answers, and source citations.
- **Authentication**: `X-Internal-Service-Key: <secret>`

---

## 4. Planned Endpoints Roadmap (Phases 11–13)

### Phase 11–13:
- Production Hardening & Security Auditing (Phase 11)
- RAG Evaluation & Benchmarking (Phase 12)
- Deployment & Continuous Integration (Phase 13)
