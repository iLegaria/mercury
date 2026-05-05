# ADR 0004: Wrap Cohere Calls with a Circuit Breaker

## Status

Accepted

## Context

Mercury depends on Cohere for embeddings and chat completions. External AI APIs can fail because of latency, rate limits, outages, invalid responses, or network problems. If every application path calls the provider directly, failures can cascade through ingestion, search, quiz generation, and RAG.

## Decision

Route Cohere API calls through a centralized `CohereClient` protected by a Resilience4j circuit breaker.

## Rationale

- Centralizes provider-specific HTTP behavior and error handling.
- Prevents repeated calls to an unhealthy external service.
- Makes provider health easier to observe through actuator metrics.
- Keeps feature services focused on business logic instead of transport details.

## Tradeoffs

- Circuit breaker settings need tuning as traffic patterns change.
- A shared breaker can temporarily affect multiple features when the provider is unhealthy.
- Fallback behavior must be explicit and user-friendly.

## Consequences

Provider failures are isolated behind one client boundary. The backend can degrade more predictably when Cohere is slow or unavailable.
