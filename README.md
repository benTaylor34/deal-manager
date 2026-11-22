# Deal Manager

A small pricing-deal workflow: a Relationship Manager (RM) builds a deal of
discounted pricing lines for a client and submits it. A discount within RM
authority (≤ 15%) is approved on submit; anything deeper goes to the Pricing
Committee, who approve or reject it with a comment. Every change is audited.

**Stack:** Spring Boot 3.5 (Java 21), Postgres 16 + Flyway, React 18 + TypeScript (Vite), docker-compose.

## Run it

```bash
docker compose up --build
```

- UI: http://localhost:3000. Use the "Acting as" menu in the header to switch between the two demo users.
- API: http://localhost:8080/api (HTTP Basic: `rm.user` / `demo` or `committee.user` / `demo`).

Local development:

```bash
docker compose up db                   # Postgres only
cd backend && ./mvnw spring-boot:run   # API on :8080
cd frontend && npm install && npm run dev   # UI on :5173, proxies /api
```

Tests: `cd backend && ./mvnw verify`. Unit tests run with plain JUnit/Mockito; `DealApiIT`
runs the full stack against a Testcontainers Postgres, so it needs Docker.

## Domain rules

```
DRAFT ──submit──► APPROVED            discount ≤ 15%: within RM authority
DRAFT ──submit──► PENDING_COMMITTEE   discount > 15%
PENDING_COMMITTEE ──approve──► APPROVED
PENDING_COMMITTEE ──reject───► REJECTED   (comment required)
REJECTED ──revise──► DRAFT
```

- **Discount** is volume-weighted across the whole deal:
  `1 − Σ(proposed × volume) / Σ(standard × volume)`, with volume = committed annual volume.
- Exactly 15.00% stays with the RM; 15.0000000001% goes to committee. The rate keeps 10
  decimal places, so nothing gets rounded under the limit.
- Pricing lines can only be added while the deal is `DRAFT`.
- The limit is configurable (`deals.approval.rm-discount-limit`).

## API

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/api/clients` | any | seeded by Flyway |
| GET | `/api/deals?status=` | any | |
| POST | `/api/deals` | RM | 201 + `Location` + `ETag` |
| GET | `/api/deals/{id}` | any | `ETag: "<version>"` |
| POST | `/api/deals/{id}/pricing-lines` | RM | `If-Match` |
| POST | `/api/deals/{id}/submit` | RM | `If-Match`, optional `{comment}` |
| POST | `/api/deals/{id}/approve` | PRICING_COMMITTEE | `If-Match`, optional `{comment}` |
| POST | `/api/deals/{id}/reject` | PRICING_COMMITTEE | `If-Match`, `{comment}` required |
| POST | `/api/deals/{id}/revise` | RM | `If-Match` |
| GET | `/api/deals/{id}/audit` | any | who / what / when / why |

Errors use RFC 9457 `ProblemDetail`:

| Status | When |
|---|---|
| 400 | bean validation (with per-field `errors`), end date not after start, reject without comment |
| 401 / 403 | not signed in / wrong role |
| 404 | unknown deal |
| 409 | illegal status transition, or two concurrent writes caught by `@Version` |
| 412 | `If-Match` version is stale (someone else changed the deal) |
| 422 | business rule, e.g. submitting a deal with no pricing lines |
| 428 | mutation sent without `If-Match` |

## Design decisions

- **Money is `BigDecimal` end to end.** Columns are `numeric(19,4)`, comparisons use
  `compareTo` (`0.150` and `0.15` are not `equals`), and JSON carries money as strings so
  JavaScript never turns it into a double. The UI only formats; all maths is on the server.
- **Rules are plain Java.** `ApprovalPolicy`, `DealStateMachine` and `DealPricingCalculator`
  have no Spring or JPA dependencies, so they are tested directly. The state-machine test
  generates every (status, action) pair, so a new status can't be left untested.
- **Optimistic locking at two levels.** `@Version` stops two transactions racing. That alone
  doesn't stop a user acting on a screen loaded ten minutes ago, so mutations also require
  `If-Match` with the version the user saw, and a mismatch returns 412.
- **Audit in the same transaction.** `AuditService.record` is
  `@Transactional(propagation = MANDATORY)`: it fails if called outside the business
  transaction, so a change and its audit row commit or roll back together. The entity is
  `@Immutable` (append-only).
- **"Auto-approved by the RM"** is read as: within RM delegated authority, the deal is approved
  on submit, and the audit row records the RM as actor plus the rule that allowed it.
- **Auth is a demo.** HTTP Basic with two in-memory users. A bank would use its identity
  provider (OIDC/SAML) with roles from entitlements, and would also enforce that whoever
  submits a deal cannot approve it.

## Not done yet / next

1. "Explain this deal" button: the backend builds a prompt from the deal data and calls an
   LLM for a short approver brief. In a real bank this would run through a governed, private
   model with logging, PII controls and human sign-off, and would only advise the approver,
   never decide.
2. `DealApproved` Kafka event, published after commit. The production version is a
   transactional outbox, so a crash between commit and publish can't lose the event.
3. Editing and removing pricing lines, pagination, and a real identity provider.

See `AI_NOTES.md` for how Claude Code was used to build this.
