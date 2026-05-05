# ADR 0002: Use RabbitMQ for Document Ingestion

## Status

Accepted

## Context

Document ingestion can be slow. A single upload may require text extraction, chunking, embedding generation, database writes, and downstream flashcard generation. Running all of that inside the upload request would make the API slower and more fragile.

## Decision

Process document ingestion asynchronously through RabbitMQ.

## Rationale

- Upload requests can return quickly after the document is accepted.
- Slow external calls, such as embedding generation, do not block HTTP request threads.
- Consumers can be scaled independently from the REST API.
- Message queues provide a natural path for retries, dead-letter queues, and operational visibility.

## Tradeoffs

- Adds infrastructure and local development complexity.
- Requires document status tracking.
- Consumers must be written with idempotency and failure handling in mind.

## Consequences

Documents move through explicit ingestion states such as `PENDING`, `PROCESSING`, `COMPLETED`, and `FAILED`. The backend behaves more like a production ingestion pipeline than a synchronous CRUD service.
