# Architecture

Mercury is a backend-focused personal knowledge engine. The backend accepts documents, processes them asynchronously, stores chunks and embeddings, and exposes RAG, quiz, flashcard, snippet, and WhatsApp review workflows.

For design tradeoffs, see the [Architecture Decision Records](adr/README.md).

## System Overview

```mermaid
flowchart LR
    Client["Client / Frontend / Browser Extension"] --> API["Spring Boot REST API"]

    API --> Postgres[("PostgreSQL + pgvector")]
    API --> Redis[("Redis")]
    API --> RabbitMQ[("RabbitMQ")]
    API --> Cohere["Cohere API"]
    API --> Storage["Local Upload Storage"]
    API --> GreenAPI["Green API WhatsApp"]

    RabbitMQ --> IngestionConsumer["Ingestion Consumer"]
    RabbitMQ --> FlashcardConsumer["Flashcard Generation Consumer"]

    IngestionConsumer --> Tika["Apache Tika"]
    IngestionConsumer --> Cohere
    IngestionConsumer --> Postgres

    FlashcardConsumer --> Cohere
    FlashcardConsumer --> Postgres
```

## Document Upload and Ingestion

Document upload is intentionally split from document processing. The HTTP request stores the document and records an outbox event; ingestion happens asynchronously through RabbitMQ.

```mermaid
sequenceDiagram
    actor User
    participant API as DocumentController
    participant Service as DocumentService
    participant DB as PostgreSQL
    participant Outbox as OutboxRelayScheduler
    participant MQ as RabbitMQ
    participant Consumer as IngestionConsumer
    participant Tika as Apache Tika
    participant Cohere as Cohere Embeddings

    User->>API: Upload file or create text document
    API->>Service: Validate request
    Service->>DB: Save document with PENDING status
    Service->>DB: Save OutboxEvent in same transaction
    Service-->>User: Return document response

    Outbox->>DB: Read unprocessed outbox events
    Outbox->>MQ: Publish DocumentIngestionEvent
    Outbox->>DB: Mark outbox event processed

    MQ->>Consumer: Deliver ingestion message
    Consumer->>Tika: Extract text when file-backed
    Consumer->>DB: Save content and chunks, mark PROCESSING
    Consumer->>Cohere: Generate chunk embeddings
    Consumer->>DB: Save embeddings and mark COMPLETED
```

## Document Status Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: document accepted
    PENDING --> PROCESSING: ingestion consumer claims document
    PROCESSING --> COMPLETED: chunks and embeddings saved
    PROCESSING --> FAILED: extraction, chunking, embedding, or persistence failure
    COMPLETED --> PENDING: content updated and re-queued
    FAILED --> PENDING: manual retry or content update
```

## RabbitMQ Topology

```mermaid
flowchart LR
    Exchange["knowledge-engine exchange"]

    Exchange -- "document.ingest" --> IngestionQ["document.ingestion"]
    IngestionQ --> IngestionConsumer["IngestionConsumer"]
    IngestionQ -- "dead-letter: document.ingest.dlq" --> IngestionDLQ["document.ingestion.dlq"]

    Exchange -- "flashcard.generate" --> FlashcardQ["flashcard.generation"]
    FlashcardQ --> FlashcardConsumer["FlashcardGenerationConsumer"]
    FlashcardQ -- "dead-letter: flashcard.generate.dlq" --> FlashcardDLQ["flashcard.generation.dlq"]
```

Both primary queues are durable and configured with dead-letter routing. This keeps failed or expired messages inspectable instead of silently dropping them.

## RAG Query Flow

RAG requests first run semantic search, then use the retrieved chunks as context for grounded answer generation.

```mermaid
sequenceDiagram
    actor User
    participant API as SearchController
    participant RAG as RAGService
    participant Search as SemanticSearchService
    participant Redis as Redis Cache
    participant Cohere as CohereClient
    participant DB as PostgreSQL + pgvector

    User->>API: POST /search/ask
    API->>RAG: ask(userId, question, collectionId)
    RAG->>Search: search top-k chunks
    Search->>Redis: Check search cache

    alt cache hit
        Redis-->>Search: Cached chunk results
    else cache miss
        Search->>Cohere: Embed query
        Cohere-->>Search: Query embedding
        Search->>DB: pgvector cosine similarity search
        DB-->>Search: Ranked chunks
        Search->>Redis: Store result with TTL
    end

    Search-->>RAG: Candidate chunks
    RAG->>RAG: Filter by minimum similarity
    RAG->>Cohere: Generate answer from retrieved context
    Cohere-->>RAG: Grounded answer
    RAG-->>API: Answer and sources
    API-->>User: RAGResponse
```

## Streaming RAG Flow

The streaming endpoint uses the same retrieval path, then emits sources first and answer tokens as they arrive.

```mermaid
sequenceDiagram
    actor User
    participant API as SearchController
    participant RAG as RAGService
    participant Cohere as CohereClient

    User->>API: POST /search/ask with Accept: text/event-stream
    API->>RAG: askStream(...)
    RAG-->>API: SSE event: sources
    RAG->>Cohere: Stream chat completion
    loop each model token
        Cohere-->>RAG: token
        RAG-->>API: SSE event: token
        API-->>User: token event
    end
    RAG-->>API: SSE event: done
    API-->>User: done event
```

## Quiz and Flashcard Flow

```mermaid
flowchart TD
    StartQuiz["Start quiz for user or document"] --> RetrieveChunks["Retrieve relevant chunks"]
    RetrieveChunks --> GenerateQuestions["Generate quiz questions with Cohere"]
    GenerateQuestions --> StoreSession["Persist quiz session and questions"]
    StoreSession --> SubmitAnswer["User submits answer"]
    SubmitAnswer --> Evaluate["Evaluate answer using selected quiz mode"]
    Evaluate --> StoreScore["Persist feedback and score"]
    StoreScore --> Incorrect{"Incorrect answer?"}
    Incorrect -- "yes" --> QuizMistakes["Add card to Quiz Mistakes deck"]
    Incorrect -- "no" --> NextQuestion["Continue quiz"]
    QuizMistakes --> NextQuestion
    NextQuestion --> Complete{"Last question?"}
    Complete -- "yes" --> Finish["Mark session completed"]
    Complete -- "no" --> SubmitAnswer
```

Flashcards use SM-2 scheduling. Review quality updates ease factor, interval, and next review date.

```mermaid
flowchart TD
    DueCards["Fetch due cards"] --> ShowFront["Show card front"]
    ShowFront --> UserRates["User rates recall quality"]
    UserRates --> SM2["Apply SM-2 scheduling"]
    SM2 --> SaveCard["Save ease factor, interval, nextReview"]
    SaveCard --> More{"More due cards?"}
    More -- "yes" --> ShowFront
    More -- "no" --> Done["Review session complete"]
```

## WhatsApp Review Flow

WhatsApp review is built on top of the flashcard scheduling model. Redis stores short-lived session state while the user answers cards through WhatsApp.

```mermaid
sequenceDiagram
    participant Scheduler as FlashcardReminderScheduler
    participant Flashcards as FlashcardService
    participant WhatsApp as WhatsAppService
    participant GreenAPI as Green API
    participant Redis as Redis
    actor User
    participant Webhook as WhatsAppWebhookController
    participant Cohere as CohereClient

    Scheduler->>Flashcards: Find due cards for reminder-enabled decks
    Scheduler->>WhatsApp: Send first due card
    WhatsApp->>GreenAPI: sendMessage
    Scheduler->>Redis: Store WhatsAppSession

    User->>GreenAPI: Reply with answer
    GreenAPI->>Webhook: Deliver inbound message
    Webhook->>Redis: Load active session
    Webhook->>Cohere: Grade answer
    Webhook->>Flashcards: Review card with quality score
    Webhook->>Redis: Advance or clear session
    Webhook->>WhatsApp: Send feedback or next card
```

## Failure Handling and Observability

```mermaid
flowchart TD
    Request["Incoming request"] --> Correlation["CorrelationIdFilter"]
    Correlation --> Logs["Logs include correlation id"]
    Correlation --> Controller["Controller"]

    Controller --> Service["Service layer"]
    Service --> External{"External dependency?"}

    External -- "Cohere" --> CircuitBreaker["Resilience4j circuit breaker"]
    CircuitBreaker --> Metrics["Actuator metrics"]
    External -- "RabbitMQ / Redis / PostgreSQL" --> Metrics

    Service --> Success["Success response"]
    Service --> Failure["Exception"]
    Failure --> Handler["GlobalExceptionHandler"]
    Handler --> ErrorResponse["Consistent API error response"]

    IngestionFailure["Ingestion failure"] --> MarkFailed["Mark document FAILED"]
    IngestionFailure --> FailureMetric["Increment failed document metric"]
```

Operational surfaces:

- `/actuator/health` for service health.
- `/actuator/metrics` for counters and dependency metrics.
- `X-Correlation-Id` for request tracing through logs.
- RabbitMQ management UI for queue and DLQ inspection.
- GitHub Actions backend CI for regression checks.

## Data Ownership

```mermaid
flowchart LR
    User["User"] --> Documents["Documents"]
    Documents --> Chunks["Chunks + embeddings"]
    Documents --> Snippets["Snippets"]
    Documents --> QuizSessions["Quiz sessions"]
    Documents --> FlashcardDecks["Flashcard decks"]
    FlashcardDecks --> FlashcardCards["Flashcard cards"]
    Documents --> Collections["Collections"]
```

Most user-facing features are scoped by `userId`, and collections provide an additional document grouping boundary for search and organization.
