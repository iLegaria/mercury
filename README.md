# Mercury - Personal Knowledge Engine

A self-hosted RAG backend that turns your documents into a queryable knowledge base. Upload PDFs, Word files, and plain text — then ask questions answered from your own content, stream answers token by token, generate LLM-graded quizzes, and study with spaced-repetition flashcards — including daily review sessions delivered directly via WhatsApp.

Built as a portfolio project to demonstrate production-grade backend engineering: async pipelines, vector search, LLM integration, resilience patterns, and observable services.

![Backend CI](https://github.com/iLegaria/mercury/actions/workflows/backend-ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL%20%2B%20pgvector-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Cohere](https://img.shields.io/badge/Cohere-Command--R-39C5BB)

![Demo](demo.gif)

---

## Features

- **RAG Q&A** — ask questions answered from your own documents (pgvector cosine search + Cohere Command-R)
- **Streaming RAG** — same pipeline with Server-Sent Events; streams answer tokens in real time via `Accept: text/event-stream`
- **Semantic search** — find relevant chunks across all uploaded content
- **LLM-graded quizzes** — STRICT (answer from context only), OPEN (semantic evaluation), or ADAPTIVE (LLM selects the best strategy per question)
- **Spaced-repetition flashcards** — SM-2 algorithm; quiz mistakes auto-added to a "Quiz Mistakes" deck; CSV import (Anki-compatible format)
- **Document snippets** — highlight and save excerpts; compile into new documents or convert directly to flashcards
- **Collections** — organize documents by project or topic for scoped search
- **Async ingestion pipeline** — upload returns instantly; PDF, Word, and plain text processed via RabbitMQ with a transactional outbox for guaranteed delivery
- **WhatsApp flashcard review** — scheduled daily reminders via Green API; answer cards directly in WhatsApp; Cohere grades replies and updates SM-2 automatically
- **Browser extension** — capture web content directly to the knowledge base

---

## Architecture

Detailed backend flows are documented in [Architecture](docs/architecture.md). Design tradeoffs are documented in [Architecture Decision Records](docs/adr/README.md).

Key backend flows:

- Document upload and async ingestion through RabbitMQ
- Transactional outbox publishing for ingestion events
- RAG query and streaming response pipeline
- Quiz generation, grading, and flashcard creation
- WhatsApp flashcard review sessions
- Failure handling, metrics, and correlation IDs

---

## Tech Stack

| Technology | Role | Why |
|---|---|---|
| Spring Boot 3.5 / Java 21 | Framework | Production-grade ecosystem; virtual threads available for async workloads |
| Cohere `embed-multilingual-v3.0` | Document & query embeddings | Multilingual support, 1024-dim vectors, separate `search_document`/`search_query` input types |
| Cohere `command-r-08-2024` | Answer generation, quiz grading, flashcard extraction, WhatsApp reply grading | Strong instruction-following; single model for all LLM tasks in the system |
| PostgreSQL + pgvector | Vector store + relational data + outbox events | Eliminates a separate vector DB; HNSW index for cosine similarity; SQL joins between docs and vectors |
| Redis | Search result cache + WhatsApp session state | 10-min TTL for search cache; TTL-scoped session store for in-progress WhatsApp review sessions |
| RabbitMQ | Async ingestion + flashcard generation queues | Two independent queues each with a DLQ; transactional outbox ensures at-least-once delivery |
| Resilience4j | Circuit breaker | Centralized in `CohereClient`; opens at 50% failure rate over 10 calls, recovers after 30s |
| Bucket4j | Rate limiting | In-memory token bucket on search endpoints; no Redis needed for single-instance rate limiting |
| Green API | WhatsApp messaging | Sends/receives WhatsApp messages via webhook; enables interactive daily flashcard review |
| Liquibase | DB migrations | Versioned schema with rollback support; 17 migration files manage full schema lifecycle |
| Testcontainers | Integration tests | Tests run against real PostgreSQL (pgvector image), RabbitMQ, and Redis — not mocks |

---

## Run in 1 Command

Requires Docker and a [Cohere API key](https://cohere.com).

```bash
# 1. Copy and fill in the environment file
cp .env.example .env
# Edit .env — required: COHERE_API_KEY, DB_PASSWORD, RABBITMQ_PASSWORD

# 2. Start everything
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API JSON spec | http://localhost:8080/api-docs |
| RabbitMQ Management | http://localhost:15672 |
| Metrics | http://localhost:8080/actuator/metrics |

---

## Development Setup

### Option A — Spring Boot auto-starts infra (recommended)

With `spring-boot-docker-compose` on the classpath, running the app automatically starts the infrastructure containers from `docker-compose.dev.yml`.

```bash
cp .env.example .env   # fill in your values
cd backend
./mvnw spring-boot:run
```

### Option B — Start infra manually, run app from IntelliJ

```bash
cp .env.example .env
docker compose -f docker-compose.dev.yml up -d

# Then run PersonalKnowledgeEngineApplication from IntelliJ
# (set environment variables from .env in the run configuration)
```

### Run tests

```bash
cd backend
./mvnw test
# Testcontainers spins up PostgreSQL, RabbitMQ, and Redis automatically — no local services needed
```

---

## Environment Variables

Copy `.env.example` to `.env` and fill in your values.

| Variable | Description | Example |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `ke_user` |
| `DB_PASSWORD` | PostgreSQL password | `change_me` |
| `DB_HOST` | PostgreSQL host (optional) | `localhost` |
| `DB_PORT` | PostgreSQL port (optional) | `5432` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `ke_user` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `change_me` |
| `RABBITMQ_HOST` | RabbitMQ host (optional) | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port (optional) | `5672` |
| `REDIS_HOST` | Redis host (optional) | `localhost` |
| `REDIS_PORT` | Redis port (optional) | `6379` |
| `COHERE_API_KEY` | Cohere API key | `sk-...` |
| `STORAGE_UPLOAD_DIR` | File upload directory (Docker only) | `/app/uploads` |
| `GREENAPI_INSTANCE_ID` | Green API instance ID (WhatsApp, optional) | `1234567890` |
| `GREENAPI_TOKEN` | Green API token (optional) | `abc123...` |
| `GREENAPI_WEBHOOK_TOKEN` | Secret to validate incoming webhooks (optional) | `my-secret` |
| `WHATSAPP_ENABLED` | Enable WhatsApp reminders (optional) | `true` |
| `WHATSAPP_USER_CHAT_ID` | Your WhatsApp chat ID (optional) | `15551234567@c.us` |
| `WHATSAPP_USER_ID` | UUID of the user to send cards to (optional) | `<uuid>` |
| `WHATSAPP_REMINDER_CRON` | Cron schedule for daily reminder (optional) | `0 0 8 * * *` |
| `WHATSAPP_MAX_CARDS` | Max cards per WhatsApp session (optional) | `10` |

---

## API Examples

Full interactive documentation at **http://localhost:8080/swagger-ui.html**.

### 1. Create a user

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email": "you@example.com", "name": "Your Name"}'
```

### 2. Upload a document

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "userId=<your-user-id>" \
  -F "title=My Document" \
  -F "file=@/path/to/document.pdf"
# Returns immediately with status=PENDING — ingestion runs async via RabbitMQ
```

### 3. Ask a question (RAG)

```bash
curl -X POST http://localhost:8080/api/v1/search/ask \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<your-user-id>",
    "question": "What are the main points of my document?"
  }'
```

### 4. Stream a RAG answer (SSE)

```bash
curl -X POST http://localhost:8080/api/v1/search/ask \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"userId": "<your-user-id>", "question": "Summarize the key arguments."}'
# Emits: {"type":"sources","chunks":[...]} → {"type":"token","text":"..."} × N → {"type":"done"}
```

### 5. Semantic search

```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<your-user-id>",
    "query": "machine learning",
    "limit": 5
  }'
```

### 6. Start a quiz

```bash
curl -X POST http://localhost:8080/api/v1/quiz/start \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<your-user-id>",
    "numQuestions": 5,
    "mode": "ADAPTIVE"
  }'
# mode: STRICT (answer must come from context) | OPEN (semantic evaluation) | ADAPTIVE (LLM picks strategy)
```

### 7. Review a flashcard

```bash
# Create a deck
curl -X POST http://localhost:8080/api/v1/flashcards/decks \
  -H "Content-Type: application/json" \
  -d '{"userId": "<your-user-id>", "name": "My Deck"}'

# Review a card — SM-2 schedules the next review based on quality
curl -X POST http://localhost:8080/api/v1/flashcards/cards/<card-id>/review \
  -H "Content-Type: application/json" \
  -d '{"userId": "<your-user-id>", "quality": 4}'
# quality: 0-2 = failed (reset interval) | 3 = passed with difficulty | 4-5 = easy
```

---

## Design Decisions

### RabbitMQ over Spring `@Async` for document ingestion

The obvious alternative to a message queue is Spring's `@Async` — it's zero infrastructure and works fine for simple cases. The reason I chose RabbitMQ instead comes down to three things:

**Durability.** If the app restarts mid-ingestion, an `@Async` task is lost. A RabbitMQ message survives restarts and gets reprocessed. For a pipeline that calls an external API (Cohere) and writes to a database, losing work silently is worse than failing visibly.

**Back-pressure.** A queue naturally limits how many documents get processed concurrently. With `@Async`, a burst of uploads would spawn concurrent threads all hammering Cohere's API simultaneously, which triggers rate limiting. The queue serializes them.

**Status tracking.** The `PENDING → PROCESSING → COMPLETED/FAILED` lifecycle is easy to implement when there's a clear handoff point (the queue). With `@Async`, you'd need to track state manually and handle the race condition between the HTTP response and the async task start.

The tradeoff: RabbitMQ is an additional service to operate. For a personal tool this is worth it — the infrastructure is already containerized.

### Transactional outbox for reliable message delivery

The naive implementation publishes directly to RabbitMQ inside the upload handler. The problem: if RabbitMQ is temporarily unavailable, the publish fails but the document row was already committed — the document is stuck in `PENDING` forever with no way to retry.

The outbox pattern solves this. On upload, the `DocumentIngestionEvent` is serialized and saved to an `outbox_events` table in the **same database transaction** as the document row. `OutboxRelayScheduler` polls every 5 minutes, reads unprocessed events, and publishes them to RabbitMQ — marking each as processed on success. The upload can succeed even if RabbitMQ is temporarily down; the event will be delivered as soon as it recovers.

The tradeoff: up to 5 minutes of delivery latency during a RabbitMQ outage. Acceptable for a personal tool where uploads are infrequent.

### pgvector over a dedicated vector database

Pinecone and Weaviate are purpose-built for vector search and scale better at tens of millions of vectors. For this use case, pgvector is the right call:

The key advantage is **SQL joins**. A semantic search query doesn't just need the most similar vectors — it needs them filtered by user, optionally by collection, and joined with document metadata for the response. With a dedicated vector DB, that means a vector query followed by a separate relational query, then merging results in application code. With pgvector, it's a single query.

The tradeoff is real: pgvector's HNSW index doesn't match the query-per-second throughput of a dedicated vector DB at scale. But "personal knowledge engine" implies one user and thousands of documents — not millions. If this were a multi-tenant SaaS product, I'd reconsider.

### Paragraph-aware chunking over fixed word splits

The original implementation split text by word count (every 500 words). The problem: it cuts mid-sentence, mid-paragraph, sometimes mid-argument. A chunk that ends abruptly in the middle of a concept produces a worse embedding because the embedding model has less context to work with.

The current chunker uses 2000-character chunks with a preference for natural split points: it looks backwards from the target position for a paragraph break (`\n\n`), then a line break, then a word boundary. It only does a hard character cut if no natural boundary exists in the second half of the chunk. Overlap is 200 characters, also character-based.

The 200-character overlap is intentional: if a key concept spans a chunk boundary, the overlap ensures it appears in full in at least one of the two chunks, improving retrieval recall.

### Centralized `CohereClient` with a single circuit breaker

Both `RAGService` and `QuizService` call Cohere's chat endpoint. The initial implementation had duplicated `WebClient` code and duplicated private record classes in each service. Beyond the obvious duplication problem, having the circuit breaker configured per-call-site means the failure window is spread across two separate services — neither accumulates enough failures to open the circuit quickly.

Centralizing all Cohere chat calls in `CohereClient` means one circuit breaker instance tracks all failures together. A Cohere outage trips the breaker faster and the fallback triggers consistently across the entire API — RAG, quiz grading, flashcard generation, and WhatsApp reply grading all fail fast together.

### SM-2 for flashcard scheduling

The [SuperMemo 2 algorithm](https://www.supermemo.com/en/blog/application-of-a-computer-to-improve-the-results-obtained-in-working-with-the-supermemo-method) is the algorithm behind Anki, the most widely used spaced-repetition tool. It adjusts review intervals using an ease factor per card: easy answers increase the factor (longer intervals), hard answers decrease it (more frequent review). The result is that each card's schedule adapts to how well you actually know it.

The alternative would be a fixed interval scheme (review after 1 day, then 3, then 7, etc.), which is simpler but ignores per-card difficulty. SM-2 is only marginally more complex to implement and produces measurably better retention. The same `reviewCard` method is called whether the review comes from the REST API or from a WhatsApp reply, keeping the scheduling logic in one place.

### Redis cache for search results

Semantic search is the most expensive operation in the system: it requires an embedding API call (Cohere, paid per token) followed by a vector similarity query. If the same user searches for the same query twice, there's no reason to repeat either operation.

The cache key is `search:{userId}:{query}:{limit}:{collectionId}` with a 10-minute TTL. The TTL is short enough that new documents (which take seconds to minutes to finish ingestion) will appear in fresh searches. The tradeoff: a user who uploads a document and immediately searches for it might see stale results for up to 10 minutes. This is acceptable for the use case — the status endpoint lets them confirm ingestion is complete before searching.

---

## Observability

Each HTTP request gets a `X-Correlation-Id` header (generated if not provided by the client) that propagates through logs via MDC. Useful for tracing a single request across log lines.

A custom `CohereHealthIndicator` pings the Cohere API on each health check, so `/actuator/health` reflects both infrastructure health (DB, Redis, RabbitMQ) and external API reachability. The Resilience4j circuit breaker state is also exposed via the health endpoint.

Custom Micrometer counters exposed at `/actuator/metrics`:
- `documents.processed` — tagged by `status=completed|failed`
- `embeddings.generated` — tagged by `type=document|query`
- `rag.queries` — total RAG questions answered

---

## Testing

45 tests — two-layer pyramid:

- **Unit tests** (Mockito, no Spring context): `RAGService`, `SemanticSearchService`, `SnippetService`, `DocumentStudyService`, `FlashcardService` (SM-2 + CSV + scheduling), `QuizService`, `ChunkingService`, `IngestionConsumer`, `IngestionPersistenceService`
- **Integration tests** (Testcontainers): real PostgreSQL + pgvector, RabbitMQ, and Redis — no infrastructure mocks

```bash
cd backend
./mvnw test
```
