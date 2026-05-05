# ADR 0003: Use Redis for Search Result Caching

## Status

Accepted

## Context

Semantic search requires embedding the query before searching similar chunks. Repeated queries from the same user can otherwise trigger duplicate embedding calls and database work.

## Decision

Cache search results in Redis with keys scoped by user, query, limit, and collection.

## Rationale

- Reduces repeated calls to the embedding provider.
- Improves latency for repeated searches.
- Keeps cached data outside the application heap.
- Supports future multi-instance deployments better than in-memory caching.

## Tradeoffs

- Adds cache invalidation concerns when documents or chunks change.
- Cached results may be briefly stale.
- Redis becomes another dependency for local and production environments.

## Consequences

Search performance improves for repeated queries, while the cache TTL limits how long stale results can survive. Document updates should evict or naturally expire affected search entries.
