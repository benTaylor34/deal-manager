# How I used AI to build this

> Rewrite these notes in your own words before the interview; they should describe *your* process.
> The entries below are from the first scaffolding session with Claude Code.

## Setup
- Wrote `CLAUDE.md` first: stack, money rules, status codes, and "rules in plain classes with
  tests". It is the contract Claude Code reads at the start of every session.
- Asked for a plan before any code and reviewed the state diagram and status-code table.

## Session 1: scaffold (Claude Code)
What it produced: the Spring Boot domain, services, REST API, Flyway schema, unit tests
(43 passing), integration test, React UI, docker-compose and CI.

Decisions worth checking, and what I checked:
- **Reading of "auto-approved by the RM".** Claude first planned a `PENDING_RM` state, then
  switched to "≤ 15% is approved on submit". Check this against the brief and be ready to
  argue for it.
- **`@Version` gap.** It pointed out that `@Version` alone doesn't catch a stale screen, and
  added an `If-Match` check that returns 412. Check: `DealServiceTest.staleVersionIsRejectedBeforeAnythingChanges`.
- **Version in the response.** Hibernate bumps the version at flush, so a response built before
  the flush would send back the old ETag. The service uses `saveAndFlush` before mapping.
- **Inverse-side collections.** Adding a `PricingLine` doesn't make the `Deal` dirty, so the
  version would not move. `Deal.addPricingLine` touches `updatedAt` so it does.
- **Rounding at the threshold.** The discount keeps 10 decimal places. At 2 places, 15.004%
  would round to 15.00% and slip under the limit.
- **Money over JSON.** `BigDecimal` is serialised as a string, so the browser never parses
  money into a double.

Environment problems: Docker Desktop failed to start on this machine, so the Testcontainers
integration test and `docker compose up` were not run in this session. Unit tests were run on
JDK 17 (`-Djava.version=17`) because JDK 21 wasn't installed.

## Template for later entries
- **Asked for:**
- **Got:**
- **Wrong / changed:**
- **How I verified:**
