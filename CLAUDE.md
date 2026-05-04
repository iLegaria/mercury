# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Personal Knowledge Engine** is a self-hosted RAG (Retrieval-Augmented Generation) system that allows users to upload documents (PDFs, Word, text) and ask questions answered from their own content. It includes semantic search, LLM-graded quizzes, and spaced-repetition flashcards.

- **Backend:** Java 21 · Spring Boot 3.5 · Maven
- **Frontend:** Next.js 16 · React 19 · TypeScript · Tailwind CSS
- **Infrastructure:** PostgreSQL + pgvector · Redis · RabbitMQ · Docker Compose
- **External APIs:** Cohere (embeddings and chat models)

## Directory Structure

```
/
├── backend/                    # Spring Boot REST API
│   ├── pom.xml                 # Maven configuration
│   ├── Dockerfile              # Multi-stage build: Maven → Java 21 JRE
│   └── src/
│       ├── main/java/ianlegaria/personalknowledgeengine/
│       │   ├── PersonalKnowledgeEngineApplication.java  # @SpringBootApplication entry point
│       │   ├── common/          # Cross-cutting concerns
│       │   │   ├── config/      # RabbitMQ, Redis, OpenAPI, Storage, Cohere configs
│       │   │   ├── filter/      # CorrelationIdFilter (MDC propagation)
│       │   │   ├── exception/   # GlobalExceptionHandler, custom exceptions
│       │   │   ├── client/      # CohereClient (centralized API calls with circuit breaker)
│       │   │   └── WebConfig.java
│       │   ├── user/            # User entity, repository, service, controller
│       │   ├── document/        # Document ingestion, text extraction
│       │   │   └── service/IngestionConsumer.java  # RabbitMQ message consumer
│       │   ├── chunk/           # Document chunking, semantic search, RAG
│       │   │   ├── service/EmbeddingService.java
│       │   │   ├── service/SemanticSearchService.java  # pgvector cosine similarity
│       │   │   ├── service/RAGService.java  # Question-answering with Cohere
│       │   │   └── controller/SearchController.java
│       │   ├── collection/      # Document collections (scoping/filtering)
│       │   ├── snippet/         # Highlighted text excerpts from chunks
│       │   ├── quiz/            # Quiz generation and grading (LLM-powered)
│       │   └── flashcard/       # Spaced-repetition flashcards (SM-2 algorithm)
│       └── test/               # Integration tests with Testcontainers
│
├── mercury-frontend/            # Next.js 16 frontend
│   ├── app/                    # App router
│   │   ├── page.tsx            # Home/dashboard
│   │   ├── layout.tsx          # Root layout with Navbar, Sidebar, Starfield canvas
│   │   ├── ask/                # RAG question page
│   │   ├── documents/          # Document management
│   │   ├── quiz/               # Quiz interface
│   │   ├── flashcards/         # Flashcard study
│   │   └── snippets/           # Snippet management
│   ├── components/
│   │   ├── ui/                 # shadcn/ui components
│   │   ├── layout/             # Navbar, Sidebar, UserGuard, StarfieldCanvas
│   │   ├── ask/                # Ask form components
│   │   ├── documents/          # Document upload/list
│   │   ├── flashcards/         # Flashcard UI
│   │   └── quiz/               # Quiz UI
│   ├── lib/                    # API clients (documents.ts, search.ts, quiz.ts, etc.)
│   ├── context/                # React context providers
│   ├── hooks/                  # Custom React hooks
│   ├── types/                  # TypeScript type definitions
│   └── package.json            # npm dependencies
│
├── browser-extension/          # Chrome/Firefox WebExtension for quick document capture
│   ├── manifest.json           # Chrome manifest
│   ├── manifest-firefox.json   # Firefox manifest
│   ├── popup.html/js           # Popup UI and logic
│   └── background.js           # Background service worker
│
├── docker-compose.yml          # Production: PostgreSQL, RabbitMQ, Redis, Backend
├── docker-compose.dev.yml      # Development: PostgreSQL, RabbitMQ, Redis (no app)
├── .env.example                # Environment variables template
└── HELP.md                     # Additional documentation

```

## Quick Start

### Prerequisites
- Java 21 (backend)
- Node.js 18+ (frontend)
- Docker & Docker Compose
- Cohere API key (get at https://cohere.com)

### Development Setup

**Option A: Auto-managed infrastructure (recommended)**

```bash
cp .env.example .env
# Edit .env — required: COHERE_API_KEY, DB_PASSWORD, RABBITMQ_PASSWORD

cd backend
./mvnw spring-boot:run
# Spring auto-starts PostgreSQL, RabbitMQ, Redis from docker-compose.dev.yml

# In another terminal:
cd mercury-frontend
npm install
npm run dev
# Open http://localhost:3000
```

**Option B: Manual infrastructure**

```bash
cp .env.example .env
docker compose -f docker-compose.dev.yml up -d

cd backend
./mvnw spring-boot:run

cd mercury-frontend
npm install
npm run dev
```

### One-Command Production Deploy

```bash
docker compose up --build
```

Access at: http://localhost:8080 (API) and http://localhost:3000 (frontend in separate deployment)

## Build & Test Commands

### Backend

```bash
cd backend

# Build
./mvnw clean package

# Run dev server (auto-starts infra)
./mvnw spring-boot:run

# Run all tests (spins up PostgreSQL, RabbitMQ, Redis via Testcontainers)
./mvnw test

# Run single test
./mvnw test -Dtest=SemanticSearchServiceIntegrationTest

# Run specific test method
./mvnw test -Dtest=SemanticSearchServiceIntegrationTest#testSearchByEmbedding

# Check dependency tree
./mvnw dependency:tree

# Skip tests during package (fast build)
./mvnw package -DskipTests
```

### Frontend

```bash
cd mercury-frontend

# Dev server (localhost:3000)
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Lint
npm run lint
```

## Architecture Patterns

### Backend Architecture

**Layered structure per feature module:**
- **Controller** → **Service** → **Repository** → **Entity**
- Example: `SearchController` → `RAGService` → `ChunkRepository` → `ChunkEntity`

**Key architectural decisions:**

1. **Document Ingestion via RabbitMQ**
   - Decouples HTTP upload from slow processing
   - Enables status tracking: `PENDING → PROCESSING → COMPLETED/FAILED`
   - `IngestionConsumer` listens for messages and processes asynchronously
   - Resilient to app restarts (messages survive)

2. **Centralized `CohereClient`**
   - All Cohere API calls (embeddings + chat) go through one client
   - Single circuit breaker (50% failure threshold, 10-call window, 30s recovery)
   - Prevents cascading failures across RAG and Quiz services

3. **Semantic Search with pgvector**
   - Documents chunked into ~2000-char segments with 200-char overlap (paragraph-aware)
   - Chunks embedded via Cohere `embed-multilingual-v3.0` (1024-dim vectors)
   - Stored in PostgreSQL with HNSW index for cosine similarity
   - SQL joins enable filtering by user/collection without separate vector DB

4. **Redis Caching**
   - Search results cached with key: `search:{userId}:{query}:{limit}:{collectionId}`
   - 10-minute TTL balances freshness vs. API cost savings
   - Avoids redundant embedding calls for repeated queries

5. **SM-2 Spaced-Repetition for Flashcards**
   - Ease factor per card adapts to performance
   - Intervals grow for easy cards, shrink for difficult ones
   - Better retention than fixed-interval schedules

### Data Model

**Core entities:**
- `UserEntity` — Account holder
- `DocumentEntity` — Uploaded file with status (PENDING/PROCESSING/COMPLETED/FAILED)
- `ChunkEntity` — Text segment with pgvector embedding
- `SnippetEntity` — Highlighted excerpt from a chunk (user bookmarks)
- `CollectionEntity` — Scoping container for documents (organize by project/topic)
- `QuizSessionEntity` + `QuizQuestionEntity` — Quiz state with mode (STRICT/OPEN)
- `FlashcardDeckEntity` + `FlashcardCardEntity` — Card scheduling with SM-2 fields

**Liquibase migrations** in `src/main/resources/db/changelog/changes/` manage schema versioning.

### Frontend Architecture

**Next.js App Router with component-driven design:**
- Pages under `app/` auto-map to routes (e.g., `app/ask/page.tsx` → `/ask`)
- `layout.tsx` wraps all pages with `Navbar`, `Sidebar`, `StarfieldCanvas`
- `UserGuard` ensures user is loaded before rendering child pages

**API clients in `lib/`:**
- `api.ts` — Base HTTP setup with error handling
- `documents.ts`, `search.ts`, `quiz.ts`, etc. — Feature-specific API methods
- All calls hit backend at `http://localhost:8080/api/v1/`

**Component organization:**
- `components/ui/` — Reusable UI primitives from shadcn/ui
- `components/layout/` — App structure (Navbar, Sidebar, StarfieldCanvas, UserGuard)
- `components/{feature}/` — Feature-specific components (ask, documents, quiz, flashcards)

**Styling:**
- Tailwind CSS with custom utility classes in `globals.css`
- Dark theme default with next-themes support
- Mercury color scheme (blues/silvers with glow effects)

### Configuration & Observability

**Environment-driven config:**
- `application.yaml` reads from `.env` (DB, RabbitMQ, Redis, Cohere credentials)
- Liquibase auto-runs migrations on startup

**Resilience4j circuit breaker:**
- `@CircuitBreaker(name = "cohereAPI")` on `CohereClient.chat()` and `embed()`
- Failure metrics exposed at `/actuator/metrics`

**Correlation IDs:**
- `CorrelationIdFilter` generates/propagates `X-Correlation-Id` header per request
- Propagated via MDC; all logs include the ID for request tracing

**Actuator endpoints:**
- `/actuator/health` — Service health (includes circuit breaker status)
- `/actuator/metrics` — Counters: `documents.processed`, `embeddings.generated`, `rag.queries`
- `/swagger-ui.html` — Interactive API docs

## Key Files to Understand

| File | Purpose |
|------|---------|
| `PersonalKnowledgeEngineApplication.java` | Spring Boot entry point |
| `chunk/service/RAGService.java` | Question-answering pipeline: embed query → search chunks → call Cohere |
| `chunk/service/SemanticSearchService.java` | pgvector cosine similarity search |
| `document/service/IngestionConsumer.java` | RabbitMQ consumer for async document processing |
| `common/client/CohereClient.java` | Centralized Cohere API calls with circuit breaker |
| `common/config/RabbitMQConfig.java` | Queue and exchange setup |
| `mercury-frontend/app/layout.tsx` | Root Next.js layout with UI shell |
| `mercury-frontend/lib/api.ts` | Base HTTP client for all frontend-backend calls |

## Dependencies & Versions

### Backend (pom.xml)
- **Spring Boot 3.5.13** on Java 21
- **PostgreSQL driver** + **Liquibase** for migrations
- **spring-boot-starter-amqp** for RabbitMQ
- **spring-boot-starter-data-redis** for caching
- **spring-boot-starter-webflux** (reactive HTTP client for Cohere)
- **resilience4j-spring-boot3** (circuit breaker)
- **bucket4j-core** (rate limiting)
- **Apache Tika 2.9.2** (document text extraction)
- **Testcontainers 1.20.4** (integration tests)
- **SpringDoc OpenAPI 2.8.8** (Swagger/OpenAPI docs)

### Frontend (package.json)
- **Next.js 16.2.4** (App Router, SSR/SSG)
- **React 19.2.4** + **React DOM 19.2.4**
- **TypeScript 5**
- **Tailwind CSS 4** + **@tailwindcss/postcss**
- **shadcn/ui** (Radix UI components + Tailwind styling)
- **next-themes** (dark mode)
- **sonner** (toast notifications)
- **lucide-react** (icons)

## Database Schema

**Key tables (managed by Liquibase):**

- `users` — ID, email, name
- `documents` — ID, user_id, title, status, created_at
- `chunks` — ID, document_id, text, embedding (pgvector), position
- `collections` — ID, user_id, name (for grouping documents)
- `snippets` — ID, chunk_id, start_pos, end_pos (bookmarked excerpts)
- `quiz_sessions` — ID, user_id, status, mode (STRICT/OPEN), created_at
- `quiz_questions` — ID, session_id, question, user_answer, correct_answer, score
- `flashcard_decks` — ID, user_id, name
- `flashcard_cards` — ID, deck_id, front, back, ease_factor, interval, next_review (SM-2)

**Vector search:**
- `chunks.embedding` is a pgvector type with HNSW index
- Queries use cosine distance: `<=> OPERATOR in SQL`

## Environment Variables

See `.env.example`. Required for both dev and prod:
- `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL credentials
- `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` — RabbitMQ credentials
- `COHERE_API_KEY` — Cohere API key
- Optional: `DB_HOST`, `DB_PORT`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `REDIS_HOST`, `REDIS_PORT`

In Docker, `STORAGE_UPLOAD_DIR=/app/uploads` must be set.

## Testing

### Backend

Integration tests use **Testcontainers** to spin up real PostgreSQL, RabbitMQ, and Redis:

```bash
cd backend

# All integration tests
./mvnw test

# Single test class
./mvnw test -Dtest=SemanticSearchServiceIntegrationTest

# Single test method
./mvnw test -Dtest=RAGServiceTest#testAnswerWithMissingChunks
```

**Key test classes:**
- `SemanticSearchServiceIntegrationTest` — pgvector cosine search
- `RAGServiceTest` — Cohere API mocking (MockWebServer)
- `QuizServiceTest` — Quiz generation and grading
- `FlashcardServiceTest` — SM-2 scheduling logic
- `IngestionConsumerTest` — Document processing pipeline

**Test base class:**
- `AbstractIntegrationTest` — Testcontainers setup, common fixtures

### Frontend

No automated test suite yet; follow manual testing during development.

## Docker Compose Services

**Production (`docker-compose.yml`):**
- `postgres:pgvector:pg16` — Vector database, port 5432
- `rabbitmq:3.13-management` — Message queue, ports 5672 (AMQP), 15672 (mgmt UI)
- `redis:7-alpine` — Cache, port 6379
- `app` — Backend Spring Boot, port 8080 (built from `backend/Dockerfile`)

**Development (`docker-compose.dev.yml`):**
- Same as above but without the app service
- `spring-boot-docker-compose` auto-starts these when running `./mvnw spring-boot:run`

## API Endpoints

Base: `http://localhost:8080/api/v1/`

**Documents:** `POST /documents/upload`, `GET /documents/{id}`, `GET /documents`
**Search:** `POST /search/ask` (RAG), `POST /search` (semantic search)
**Quiz:** `POST /quiz/start`, `POST /quiz/sessions/{id}/answer`
**Flashcards:** `POST /flashcards/decks`, `POST /flashcards/cards/{id}/review`
**Collections:** `POST /collections`, `GET /collections`
**Users:** `POST /users`, `GET /users/{id}`

Full interactive docs at `/swagger-ui.html`.

## Browser Extension

The `browser-extension/` directory contains a minimal Chrome/Firefox extension for capturing web content. Manifests support both browsers; send captured text to the backend's document ingestion endpoint.

## Common Development Tasks

**Update a dependency version:**
```bash
# Backend: edit backend/pom.xml version in <dependency>
# Frontend: npm update <package-name> or edit package.json version

# Backend: run integration tests to verify
./mvnw test

# Frontend: npm run build
```

**Add a new feature module:**
1. Create folder under `backend/src/main/java/ianlegaria/personalknowledgeengine/{feature}/`
2. Structure: `entity/`, `repository/`, `service/`, `controller/`, `dto/`
3. Create Liquibase migration in `backend/src/main/resources/db/changelog/changes/`
4. Wire up in `common/config/` if needed (queues, caching, etc.)
5. Add integration tests in `backend/src/test/`

**Add a new frontend page:**
1. Create directory under `mercury-frontend/app/{route}/`
2. Add `page.tsx` (auto-routed)
3. Create components in `mercury-frontend/components/{route}/`
4. Add API client methods in `mercury-frontend/lib/{feature}.ts` if needed
5. Reference in layout or sidebar navigation

**Debug embedding/search quality:**
- Check `CohereClient` for rate limits or API errors in logs (circuit breaker state)
- Verify chunk quality: inspect `chunks` table, review text extraction in `TextExtractionService`
- Check Redis cache hits: monitor via `/actuator/metrics` counters
- Test pgvector query directly: `SELECT * FROM chunks ORDER BY embedding <-> embedding('...') LIMIT 5;`

## Notes for Future Work

- **Frontend tests:** Consider adding Jest/React Testing Library for components and hooks
- **Rate limiting:** Currently in-memory per-instance; move to Redis for multi-instance deployments
- **Multi-tenancy:** Database schema already supports `user_id` scoping; consider adding org-level grouping
- **Vector DB scaling:** If >10M vectors, consider dedicated vector DB (Pinecone, Qdrant) instead of pgvector

