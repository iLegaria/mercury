# ADR 0001: Use PostgreSQL and pgvector for Semantic Search

## Status

Accepted

## Context

Mercury needs semantic search over document chunks while also storing relational data for users, documents, collections, snippets, quizzes, and flashcards. A dedicated vector database would work, but it would add another service to operate and another consistency boundary between vectors and metadata.

## Decision

Store embeddings in PostgreSQL using the pgvector extension and query them with cosine similarity.

## Rationale

- Keeps document metadata and vector data in the same database.
- Allows user, collection, and document filtering with normal SQL joins.
- Reduces infrastructure complexity for a self-hosted system.
- Demonstrates practical vector search without requiring a managed vector database.

## Tradeoffs

- A dedicated vector database may scale better at very high vector counts.
- Query performance depends on correct indexing and database tuning.
- PostgreSQL carries both transactional and vector-search workloads.

## Consequences

The backend can support semantic search with a simpler operational model. If the system grows beyond PostgreSQL and pgvector's practical limits, the vector storage boundary can be revisited.
