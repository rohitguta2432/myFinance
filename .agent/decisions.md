# Architecture Decisions

Decisions made for this project. Do NOT suggest alternatives to these unless explicitly asked.

## ADR-001: Zustand over Redux
**Decided**: Zustand with `persist` middleware.
**Why**: Single-user financial wizard — Redux is overkill. Zustand's `persist` auto-syncs to localStorage with zero boilerplate. Store is ~114 lines vs what would be 500+ with Redux slices + actions.

## ADR-002: No Spring Security (Yet)
**Decided**: Skip authentication until MVP is validated.
**Why**: Users enter financial data locally via the wizard. No login needed. Data persists in browser localStorage. Will add auth before multi-user or cloud-sync features.

## ADR-003: H2 for Dev, PostgreSQL for Prod
**Decided**: Dual database strategy.
**Why**: H2 in-memory gives instant restarts in dev with zero setup. Docker Compose handles PostgreSQL for prod. Spring profiles + env var overrides handle the switch. No migration tool (Flyway/Liquibase) yet — using `ddl-auto: update`.

## ADR-004: Native Fetch over Axios
**Decided**: Custom fetch wrapper in `src/services/api.js`.
**Why**: Zero dependencies. 40 lines of code. Handles JSON headers, error parsing, 204 No Content. No need for interceptors, cancellation, or other axios features.

## ADR-005: Tailwind CSS v4 over Vanilla CSS
**Decided**: Tailwind v4 + `clsx` + `tailwind-merge`.
**Why**: Utility-first approach matches component-based React architecture. `tailwind-merge` prevents class conflicts. Vite plugin (`@tailwindcss/vite`) for zero-config integration.

## ADR-006: Lombok over Java Records
**Decided**: Lombok `@Data`, `@Builder` for all DTOs and entities.
**Why**: Records are immutable — don't work well with JPA entities (Hibernate needs no-arg constructor + setters). Lombok gives builder pattern + equals/hashCode + toString for free.

## ADR-007: AWS Bedrock over OpenAI
**Decided**: Amazon Nova Lite (`amazon.nova-lite-v1:0`) via AWS SDK.
**Why**: Already on AWS (EC2 deployment). No separate API key management — uses IAM credential chain. Cost-effective for the chat use case.

## ADR-008: Feature-Based Folder Structure
**Decided**: `src/features/assessment/` and `src/features/dashboard/` with shared `src/hooks/`.
**Why**: Co-locates related pages/store/components. Shared domain hooks (tax, insurance, health score) used across both features live in `src/hooks/`.

## ADR-009: Domain Logic in Custom Hooks
**Decided**: All financial calculations in dedicated hooks (not in components).
**Why**: `useTaxAnalysis` (275 lines), `useFinancialHealthScore`, `useInsuranceAnalysis` etc. are complex. Keeping them in hooks makes them testable, reusable, and keeps components thin.

## ADR-010: Docker Compose 3-Service Architecture
**Decided**: `postgres` + `backend` + `frontend` as separate containers.
**Why**: Clean separation. Frontend served via nginx. Backend connects to postgres via Docker networking. Single `docker-compose up` for full stack.
