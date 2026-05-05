# ADR 0005: Use Testcontainers for Integration Tests

## Status

Accepted

## Context

The backend depends on PostgreSQL with pgvector, RabbitMQ, and Redis. Mocking those dependencies would miss important behavior such as SQL compatibility, migrations, queue configuration, cache behavior, and container startup assumptions.

## Decision

Use Testcontainers for integration tests that need real infrastructure.

## Rationale

- Tests run against real PostgreSQL, RabbitMQ, and Redis instances.
- Liquibase migrations are validated against the same database engine used in development.
- Integration behavior is closer to production than pure mocks.
- GitHub Actions can run the same tests in CI using Docker.

## Tradeoffs

- Tests are slower than pure unit tests.
- Docker is required to run the full suite locally.
- CI must provide a Docker-capable runner.

## Consequences

The backend gets stronger regression coverage for infrastructure-dependent behavior. Developers without Docker can still run focused unit tests, while CI remains responsible for the complete backend verification.
