# Deal Manager — notes for Claude Code

A small take on a bank's pricing deal workflow: an RM builds a deal of discounted
pricing lines for a client, submits it, and it is approved within RM authority or
routed to the Pricing Committee.

## Stack
- `backend/`: Spring Boot 3.5, Java 21, Maven (`./mvnw`), Postgres 16, Flyway, JPA.
- `frontend/`: React 18 + TypeScript + Vite, TanStack Query, React Router.
- `docker-compose.yml` runs db + backend + frontend.

## Non-negotiable rules
- **Money is `BigDecimal`**, never `double`/`float`. Compare with `compareTo`, never
  `equals` (scale matters to `equals`). Money columns are `numeric(19,4)`.
  Money goes over the wire as JSON **strings** so JavaScript never rounds it.
- **Business rules live in plain classes** with no Spring/JPA:
  `ApprovalPolicy` (threshold) and `DealStateMachine` (transitions). Every change to
  them comes with unit tests.
- **Every state-changing service method writes an `AuditEvent`** in the same
  transaction: actor, action, from/to status, reason, details.
- **Optimistic locking**: mutating endpoints require `If-Match: "<version>"`.
  Missing → 428, mismatch → 412, concurrent clash at flush → 409.
- Status codes: 201 + `Location` on create, 400 validation, 403 wrong role,
  404 missing, 409 illegal transition, 422 business rule (e.g. submit with no lines).
  Errors are RFC 9457 `ProblemDetail`.
- Schema changes go in a new Flyway migration; Hibernate only validates.
- Open-session-in-view is off: map entities to DTOs inside the transaction.

## Domain rules
- Discount = `1 − Σ(proposed × volume) / Σ(standard × volume)` across the whole deal.
- Discount ≤ 15% → auto-approved on submit (within RM authority).
  Discount > 15% → `PENDING_COMMITTEE`; only `PRICING_COMMITTEE` may approve/reject.
- States: `DRAFT → (submit) → APPROVED | PENDING_COMMITTEE`,
  `PENDING_COMMITTEE → APPROVED | REJECTED`, `REJECTED → (revise) → DRAFT`.
  Pricing lines are editable only in `DRAFT`. Reject requires a comment.

## Commands
- Backend tests: `cd backend && ./mvnw verify` (integration test needs Docker).
- Frontend: `cd frontend && npm install && npm run dev` (proxies `/api` to :8080).
- Everything: `docker compose up --build` → UI on http://localhost:3000.

## Working agreement
- Plan first for anything touching the domain model; keep diffs small.
- Log notable AI decisions/mistakes in `AI_NOTES.md`.
