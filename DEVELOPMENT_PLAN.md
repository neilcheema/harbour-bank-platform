# Development Plan: Harbour Digital Banking Platform

A 10-week core plan plus 2 optional weeks. Assumes roughly **8 to 10 hours per week** alongside a full-time job. If you have less time, stretch each week to two; the order matters more than the pace.

**Working rules for every week**
- Work in small pull requests against your own repo, one per feature, with a clear description. Squash-merge so history reads well.
- Write the **domain logic and its tests yourself first**, then use AI to review, suggest edge cases, and explain Java idioms. Never merge code you can't explain.
- Keep a running `docs/ai-workflow.md` log (see template at the end).
- End each week with a 15-minute retro note in `docs/journal.md`: what you learned, what was hard, what's next.
- Tag a release at the end of each week (`v0.1.0`, `v0.2.0`, ...). It makes progress visible on GitHub.

---

## Overview

| Week | Theme | Headline deliverable |
|---|---|---|
| 0 | Setup | Repo, tooling, CI skeleton |
| 1 | Java 21 and Spring Boot foundations | Running app with customers and accounts CRUD |
| 2 | Persistence and schema | Flyway schema, JPA, Testcontainers |
| 3 | Ledger core | Money type and balanced double-entry ledger |
| 4 | Transfers | Idempotent, concurrency-safe transfers |
| 5 | Security | OAuth2/JWT, ownership, audit trail |
| 6 | Events | Outbox, Kafka, notifications |
| 7 | Core banking adapter and fraud rules | Port and adapter, rules engine |
| 8 | Angular client | Login, accounts, transfers, history |
| 9 | Quality and observability | Concurrency/load tests, metrics, ArchUnit |
| 10 | Cloud and polish | Azure deploy, docs, demo |
| 11-12 | Optional stretch | AI insights, resilience, cloud core adapter |

---

## Week 0: Setup (1 to 2 evenings)

**Goals:** a clean foundation so every later week is about features.

**Tasks**
- Install JDK 21, Maven (or Gradle), Docker, IntelliJ IDEA (Community is fine), Node 20+.
- Create the GitHub repo `harbour-bank-platform`; add the README and this plan; add `LICENSE` (MIT or Apache-2.0), `.gitignore`, and `CODEOWNERS` if you like.
- Generate the Spring Boot 3 project (Spring Initializr): Web, Validation, Data JPA, PostgreSQL, Flyway, Actuator, Security, OAuth2 Resource Server, Testcontainers, Lombok optional (prefer records and plain Java for learning).
- `docker-compose.yml` with PostgreSQL.
- GitHub Actions: build and run tests on every push.
- Branch protection on `main`; require CI to pass.

**Deliverable:** green CI on an empty Spring Boot app. Tag `v0.0.1`.

**AI tip:** ask Claude Code to explain the Spring Boot project layout and each dependency you added, then quiz you on it.

---

## Week 1: Java 21 and Spring Boot foundations

**Goals:** get comfortable with modern Java and the Spring programming model, coming from C#/.NET.

**Learn**
- Records, sealed interfaces, `Optional`, streams (compare with LINQ), `var`, text blocks, pattern-matching `switch`.
- Spring DI (compare with .NET DI), `@Configuration`, `@Bean`, profiles, `@ConfigurationProperties`.
- Maven lifecycle and dependency management.

**Build**
- `customers` and `accounts` modules with domain classes, services, and REST controllers.
- DTOs as records; mapping by hand (skip MapStruct for now to learn the mechanics).
- Global exception handler returning RFC 7807 `ProblemDetail`.
- Bean Validation on request DTOs.

**Deliverable:** `GET/POST /api/v1/customers`, `GET/POST /api/v1/accounts` working against an in-memory store or H2. Unit tests for services. Tag `v0.1.0`.

**Definition of done:** you can explain how Spring wires a controller to a service, and how it differs from .NET.

---

## Week 2: Persistence and schema

**Goals:** a real database with versioned migrations and real-database tests.

**Learn**
- JPA/Hibernate basics: entities, relationships, lazy vs eager loading, the N+1 problem, `@Version`.
- Flyway migrations; why `ddl-auto=none`.
- Testcontainers with `@ServiceConnection`.

**Build**
- `V1__init.sql`: customers, accounts, payees.
- Spring Data repositories; custom queries with `@Query` and projections.
- Pagination and sorting for list endpoints.
- Repository tests with `@DataJpaTest` plus Testcontainers PostgreSQL.

**Deliverable:** persistent customers and accounts; migrations run on startup; repository tests green. Tag `v0.2.0`.

**AI tip:** paste an entity and ask Claude to find N+1 risks and missing indexes; verify its claims by enabling SQL logging.

---

## Week 3: Ledger core (the heart of the project)

**Goals:** correct money handling and a double-entry ledger.

**Learn**
- Why `BigDecimal`, scale, and rounding modes; banker's rounding.
- Double-entry accounting basics: debits, credits, balanced journals, reversing entries.

**Build**
- `Money` record with arithmetic and currency checks (section 3.2 of the README).
- `JournalEntry`, `Posting`, and the balancing invariant.
- `V2__ledger.sql`: `journal_entry`, `posting`; constraint-level safeguards where possible.
- `LedgerService.post(JournalEntry)` updates cached balances in the same transaction.
- Transaction history endpoint with filters and pagination.
- **Property-based tests (jqwik):** any random set of valid journal entries keeps total debits equal to total credits.

**Deliverable:** you can post entries and read balances and history; ledger invariants proven by tests. Tag `v0.3.0`.

**Definition of done:** write the unit tests *before* asking AI for review. Aim for 90%+ coverage on `ledger.domain`.

---

## Week 4: Transfers

**Goals:** move money safely under retries and concurrency.

**Learn**
- Transaction isolation levels, optimistic vs pessimistic locking, deadlocks and lock ordering.
- Idempotency patterns for APIs.

**Build**
- `POST /transfers` with a required `Idempotency-Key` header; unique constraint on the key; return the stored result on replay.
- `TransferService`: validate, lock accounts in deterministic order, build the journal entry, post, set status.
- Domain exceptions mapped to proper HTTP statuses (422 for insufficient funds, 409 for conflicts).
- Failure paths: closed/frozen accounts, same-account transfer, currency mismatch.

**Test**
- Integration test: same key sent 10 times in parallel produces exactly one transfer.
- Integration test: 200 concurrent transfers among 5 accounts; total money conserved; no negative balances on non-overdraft accounts; no deadlocks.

**Deliverable:** reliable transfer endpoint. Tag `v0.4.0`.

**AI tip:** after your implementation passes, ask Claude to try to *break* it by proposing race conditions; write a test for each credible one.

---

## Week 5: Security

**Goals:** bank-grade API security basics.

**Learn**
- OAuth2 resource server, JWT validation, claims, roles vs ownership (authorization is not just authentication).
- OWASP API Security Top 10, especially broken object-level authorization.

**Build**
- JWT validation; run a local Keycloak in Docker Compose (preferred) or a dev token endpoint.
- Roles `CUSTOMER`, `AUDITOR`, `ADMIN`; method security with `@PreAuthorize`.
- `OwnershipGuard`: every account/transfer access verifies the account belongs to the caller; return 404 for non-owned resources.
- Audit trail: an `AuditEvent` written for every state-changing request (actor, action, resource, timestamp, correlation id).
- Rate limiting on transfer and auth endpoints (Bucket4j).
- Security headers, CORS config for the Angular origin, PII masking in logs.

**Test**
- Security tests proving user A cannot read or transfer from user B's accounts (a classic BOLA test suite).
- Auditor can read audit logs; customers cannot.

**Deliverable:** secured API, security notes in `docs/security.md`. Tag `v0.5.0`.

---

## Week 6: Events (outbox, Kafka, notifications)

**Goals:** reliable event-driven integration, a common bank architecture.

**Learn**
- Kafka basics: topics, partitions, consumer groups, offsets, at-least-once delivery.
- Dual-write problem and the **transactional outbox** pattern.
- Idempotent consumers.

**Build**
- `outbox_event` table; written in the same transaction as the transfer.
- Outbox relay (scheduled poller) publishes to Kafka and marks events published.
- `notifications` module: consumes `TransferCompleted` and `LowBalanceDetected`, stores notifications, exposes `GET /notifications`.
- Consumer idempotency via processed-event ids; dead-letter topic for poison messages.
- Add Kafka (KRaft mode) to Docker Compose.

**Test:** Testcontainers Kafka integration test: complete a transfer, assert a notification appears exactly once even if the event is delivered twice.

**Deliverable:** end-to-end event flow. Tag `v0.6.0`.

---

## Week 7: Core banking adapter and fraud rules

**Goals:** demonstrate the migration-friendly architecture that fits your Tangerine/Scotiabank story.

**Learn**
- Hexagonal architecture (ports and adapters), anti-corruption layers.
- Resilience patterns: timeouts, retries with backoff, circuit breakers.

**Build**
- `CoreBankingGateway` port and a `MockCoreBankingAdapter` that simulates a legacy core (latency, occasional failures, different field names and formats).
- An anti-corruption mapper translating core DTOs to your domain model.
- Configuration switch (`harbour.core.adapter=mock|cloud`) via Spring profiles.
- Resilience4j circuit breaker, retry, and time limiter around the adapter.
- `fraud` module: rule interface plus rules (velocity, unusual amount vs history, new payee plus large amount). Output a score and create `FraudAlert`; hold transfers above threshold.

**Test:** adapter contract tests (the same suite runs against every adapter implementation); fraud rule unit tests with labeled synthetic scenarios.

**Deliverable:** swappable core integration and a rules engine. Tag `v0.7.0`.

**Why this matters for you:** you can truthfully say "I built the abstraction layer that lets the channel layer survive a core replacement", which is exactly the Tangerine scenario, without referencing any employer system.

---

## Week 8: Angular client

**Goals:** a usable front end that proves the API end to end. Reuse your Angular strength; keep the Java the focus.

**Build**
- Auth flow (OIDC code flow with PKCE against Keycloak) and an HTTP interceptor.
- Screens: login, account overview, transaction history (filter/paginate), new transfer (with idempotency key generated per submit), notifications, statements list.
- Typed API client generated from `openapi.yaml` (openapi-generator).
- Basic accessibility pass (keyboard, labels, contrast).
- Playwright smoke test for the login-to-transfer path.

**Deliverable:** working UI against local stack, with screenshots for the README. Tag `v0.8.0`.

**AI tip:** let Claude Code scaffold components, but review the auth and error handling by hand.

---

## Week 9: Quality and observability

**Goals:** make it look and behave like production-grade work.

**Build**
- ArchUnit tests enforcing module boundaries and no cycles.
- Actuator health groups (liveness/readiness), Micrometer custom metrics (transfers completed/failed, outbox lag).
- Prometheus and Grafana in Docker Compose; commit a dashboard JSON.
- k6 load test script for transfers; record results in `docs/performance.md` (p95 latency, throughput, what you tuned).
- Add a database index review and fix any slow queries found with `EXPLAIN ANALYZE`.
- Mutation testing with PIT on the ledger domain (optional but impressive).
- Static analysis: SpotBugs or Error Prone; formatting with Spotless.
- Dependency and image scanning in CI.

**Deliverable:** dashboards, load-test report, quality gates in CI. Tag `v0.9.0`.

---

## Week 10: Cloud deployment and polish

**Goals:** ship it and make the repo shine.

**Build**
- Production Dockerfile (multi-stage, non-root user, layered jars).
- Deploy to **Azure Container Apps** (or AKS if you want more depth): managed PostgreSQL, Key Vault for secrets, Application Insights or OpenTelemetry export. Use Bicep or Terraform (or `azd`) so deployment is code.
- CD pipeline: on tag, build, scan, push image, deploy to a staging environment.
- Seed script to load the demo dataset.
- Finish documentation: ADRs (10 listed in README), `security.md`, `performance.md`, `ai-workflow.md`.
- Record a 3-minute demo video or GIF; add screenshots and badges (build, coverage).
- Write a short blog or LinkedIn post: what you built, what you learned, what you'd do differently.

**Deliverable:** public repo plus live demo (optionally with a throwaway demo login). Tag `v1.0.0`.

**Cost note:** use small/consumption tiers and tear down when not demoing. Set an Azure budget alert.

---

## Optional Weeks 11 to 12: Stretch

Pick based on interest and the roles you're aiming for.

| Option | What you build | What it signals |
|---|---|---|
| AI insights | `/accounts/{id}/insights` using the Claude API; mask PII first; cache responses; log prompt/response metadata; add an evaluation test set | Responsible AI integration in banking |
| Cloud core adapter | Second adapter for a REST-based core; OpenAPI-generated client; same contract test suite | Migration expertise |
| Statements | PDF/CSV statement generation with scheduled monthly job and interest posting | Batch processing know-how |
| Multi-currency | FX rates, conversion postings, rounding rules | Ledger depth |
| GraphQL | Read-optimized API for dashboards | API breadth |
| Spring Batch | End-of-day processing job (interest, reconciliation against the mock core) | Classic banking batch work |

---

## Milestone checklist

- [ ] v0.1.0 CRUD API
- [ ] v0.2.0 Persistence with Flyway and Testcontainers
- [ ] v0.3.0 Ledger invariants proven
- [ ] v0.4.0 Idempotent, concurrency-safe transfers
- [ ] v0.5.0 JWT security and audit trail
- [ ] v0.6.0 Outbox and Kafka notifications
- [ ] v0.7.0 Core adapter and fraud rules
- [ ] v0.8.0 Angular client
- [ ] v0.9.0 Observability and quality gates
- [ ] v1.0.0 Deployed with documentation

---

## Risk and mitigation

| Risk | Mitigation |
|---|---|
| Scope creep | Stick to the weekly deliverable; park ideas in a `docs/backlog.md` |
| Falling behind | Weeks 8 (UI) and 9 (polish) can shrink; never skip weeks 3 to 5 |
| Over-relying on AI | Domain code and tests are hand-written first; log AI use and verification |
| Cloud cost surprises | Budget alert; tear down environments; use free tiers |
| Accidentally exposing employer info | Fictional bank, synthetic data, no internal system names or schemas anywhere in the repo |

---

## `docs/ai-workflow.md` template

```markdown
# AI-assisted development log

## Principles
- AI proposes; I decide. I can explain every line I merge.
- Domain logic and tests are written by me first; AI reviews and challenges.
- Every AI-generated change is run, tested, and read before merge.

## Log
| Date | Task | Tool | What AI did | What I verified | Issue found / lesson |
|------|------|------|-------------|-----------------|----------------------|
| 2026-12-01 | Idempotent transfer design | Claude | Suggested unique key plus stored response | Wrote concurrency test | Initial suggestion missed in-flight duplicate; fixed with insert-first pattern |

## Prompts that worked
- ...

## Where AI was wrong
- ...
```

---

## How to present this on GitHub and LinkedIn

- Pin the repo; add topics: `java`, `spring-boot`, `banking`, `kafka`, `angular`, `testcontainers`, `ddd`, `hexagonal-architecture`.
- README top section: one-paragraph pitch, architecture diagram, screenshots, "run it in 2 minutes".
- Write a short case study: *"Designing a core-banking-agnostic digital channel layer in Java"*, covering the adapter pattern, the ledger invariants, and the concurrency test.
- Mention the AI workflow explicitly. Showing disciplined, verified AI use is a differentiator.
