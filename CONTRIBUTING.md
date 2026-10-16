# Contributing to LedgerFlow

Thanks for your interest in contributing! This guide gets you from zero to an open PR in under 10 minutes.

## Quick start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop (required for Testcontainers — tests spin up a real PostgreSQL)

### Fork and clone

```bash
# Fork via GitHub UI, then:
git clone https://github.com/<your-username>/ledgerflow.git
cd ledgerflow/ledgerflow
```

### Run the tests

```bash
mvn test
```

This starts a real PostgreSQL container via Testcontainers, runs Flyway migrations against it, and exercises the full API. All tests must pass before you open a PR.

### Run the app locally

```bash
docker compose up --build
```

Then hit `http://localhost:8080/swagger-ui.html` to explore the API.

---

## Picking an issue

Look for issues labelled **[`good first issue`](https://github.com/taniyanandi/ledgerflow/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)**. Each one:

- Points to the exact file(s) to change
- Has clear acceptance criteria
- Is scoped to a single, self-contained change

Comment on the issue to claim it before starting — so two people don't work on the same thing.

---

## Branch and commit conventions

```bash
# Branch off main
git checkout main
git pull origin main
git checkout -b fix/payment-events-fk    # or feat/list-accounts-endpoint

# Make your changes, then commit
git add <files>
git commit -m "fix: add FK constraint on payment_events.payment_id"
```

Branch naming: `feat/<short-description>`, `fix/<short-description>`, `test/<short-description>`, `docs/<short-description>`.

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `test:`, `docs:`, `refactor:`.

---

## Opening a pull request

1. Push your branch: `git push origin <branch-name>`
2. Open a PR against `main` on the upstream repo
3. In the PR description, reference the issue: `Closes #<number>`
4. The CI pipeline (CircleCI) will run `mvn test` — wait for it to go green

A maintainer will review within a few days. For Hacktoberfest, make sure your PR is not a trivial change (docs typos don't count) — the issues labelled `hacktoberfest` are pre-approved as meaningful contributions.

---

## Code style

- Standard Java naming conventions
- No `float` or `double` for money — use `long` (minor units) or `BigDecimal`
- Spring Boot patterns already in the codebase — match the existing style
- New endpoints need: a controller method, a service method, and ideally an integration test in `src/test/`
- Flyway migrations go in `src/main/resources/db/migration/` — name them `V<next>__<description>.sql`

---

## Need help?

Open a GitHub Discussion or leave a comment on the issue. Happy to answer questions about the architecture, the saga pattern, or how Testcontainers works here.
