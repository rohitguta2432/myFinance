# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Personal finance assessment and advisory platform for Indian users. 6-step wizard (profile, cash flow, net worth, goals, insurance, tax) with AI-powered insights via AWS Bedrock. Single-tenant MVP deployed on AWS EC2.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + Vite 7 + Tailwind CSS 4 |
| State | Zustand (auth + assessment persistence) + TanStack React Query v5 (server state) |
| Backend | Spring Boot 3.4.1, Java 21, Maven |
| Database | PostgreSQL 14 |
| ORM | Hibernate/JPA (ddl-auto: update — no Flyway migrations) |
| Auth | Google OAuth 2.0 (ID token verification) |
| AI | AWS Bedrock (Amazon Nova Lite) |
| Containers | Docker + Docker Compose |
| Web Server | Nginx (reverse proxy + SSL via Certbot in production) |

## Project Structure

```
myFinance/
├── backend/                  # Spring Boot backend
│   ├── src/main/java/com/myfinance/
│   │   ├── controller/       # REST controllers (/api/v1/*)
│   │   ├── service/          # Business logic
│   │   │   └── dashboard/    # 12 calculator services for dashboard
│   │   ├── model/            # JPA entities
│   │   │   └── enums/        # Frequency, MaritalStatus, EmploymentType, etc.
│   │   ├── dto/              # Data transfer objects
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── util/             # EnumUtils, JsonUtils
│   │   └── config/           # CORS, OpenAPI/Swagger config
│   └── src/test/java/com/myfinance/  # Mirrors main — *Test.java suffix
├── src/                      # React frontend
│   ├── features/             # Feature modules
│   │   ├── auth/             # Google OAuth (store, pages)
│   │   ├── assessment/       # 6-step wizard (pages, services, store)
│   │   └── dashboard/        # Financial dashboard
│   ├── components/           # Shared UI (Layout, ProtectedRoute, AiChatWidget)
│   ├── hooks/                # Dashboard query hooks
│   ├── services/             # API client (native fetch)
│   ├── constants/            # Routes, enums
│   ├── styles/               # Tailwind theme (index.css with @theme)
│   └── utils/                # Formatters (Indian locale)
├── specs/                    # Architecture & business specs (per-step + cross-cutting)
├── docker-compose.yml        # Full-stack orchestration
├── nginx.conf                # Reverse proxy + SSL config
└── deploy_aws.sh             # EC2 deployment script
```

## Commands

### Running Locally

Use the `/start` skill which handles both services, or manually:

```bash
# Backend (port 8081) — requires PostgreSQL on localhost:5432
cd backend && mvn spring-boot:run

# Frontend (port 3005, proxies /api to :8081) — requires Node 22 via nvm
nvm use 22 && npm run dev
```

### Testing

```bash
# All backend tests
cd backend && mvn test

# Single test class
cd backend && mvn test -Dtest=ProfileServiceTest

# Single test method
cd backend && mvn test -Dtest=ProfileServiceTest#getProfile_returnsEmptyDTO_whenNotFound

# Coverage report (JaCoCo enforces 80% line coverage minimum)
cd backend && mvn clean test
# Report at backend/target/site/jacoco/index.html
```

### Linting & Formatting

```bash
# Frontend ESLint
npm run lint

# Backend code formatting (Palantir Java Format via Spotless)
cd backend && mvn spotless:check    # check only
cd backend && mvn spotless:apply    # auto-fix
```

### Build & Deploy

```bash
npm run build                              # Frontend → dist/
cd backend && mvn clean package -DskipTests # Backend → JAR
docker compose up -d --build               # Full stack
./deploy_aws.sh                            # EC2 deploy
```

## Ports

| Service | Local Dev | Docker |
|---------|-----------|--------|
| Frontend | 3005 | 80/443 |
| Backend | 8081 | 8080 → 8081 |
| PostgreSQL | 5432 | 5432 |

## Architecture

### Assessment Flow (6 Steps → Dashboard)

Each step has a parallel structure across frontend and backend:

```
Frontend Step Page  →  assessmentApi.js mapper  →  POST /api/v1/{resource}  →  Controller  →  Service  →  Repository  →  Entity
     ↕                                                                                          ↓
Zustand store                                                                          Business calculations
(localStorage)                                                                         (risk scoring, tax calc, etc.)
```

| Step | Frontend Page | Backend Endpoint | Entity |
|------|--------------|-----------------|--------|
| 1. Profile & Risk | Step1PersonalRisk.jsx | /api/v1/assessment/profile | Profile |
| 2. Income & Expenses | Step2IncomeExpenses.jsx | /api/v1/assessment/financials | Income, Expense |
| 3. Assets & Liabilities | Step3AssetsLiabilities.jsx | /api/v1/assessment/balance-sheet | Asset, Liability |
| 4. Financial Goals | Step4FinancialGoals.jsx | /api/v1/assessment/goals | Goal |
| 5. Insurance Gap | Step5InsuranceGap.jsx | /api/v1/assessment/insurance | Insurance |
| 6. Tax Optimization | Step6TaxOptimization.jsx | /api/v1/assessment/tax | Tax |

### Dashboard Calculator Pattern

`DashboardService` orchestrates 12 calculator services that produce the dashboard summary from a single `/api/v1/dashboard/summary` endpoint. `DashboardDataLoader` fetches all user data once, then each calculator (HealthScoreCalculator, ProjectionCalculator, RedFlagsCalculator, etc.) processes its slice independently. All output is composed into `DashboardSummaryDTO` with nested static DTOs.

### Dual State Management (Frontend)

- **Zustand stores** (`useAuthStore`, `useAssessmentStore`): Persisted to localStorage for offline-capable form state, session management (7-day expiry)
- **TanStack Query hooks** (`useProfileQuery`, `useFinancialsQuery`, `useDashboardSummary`, etc.): Server state with 5-30min stale times, automatic invalidation on mutations
- **Pattern**: Zustand holds form edits optimistically; TanStack Query syncs with backend and invalidates on mutation success

### DTO ↔ Entity Mapping

Manual mapping (no MapStruct). Frontend `assessmentApi.js` has `mapXToDTO()`/`mapXFromDTO()` converters for enum alignment. Backend services use builder pattern with `EnumUtils.safeEnum()` (String→Enum) and `JsonUtils.toJson()`/`fromJson()` for JSON-in-TEXT-column fields (e.g., `riskAnswers`).

### Auth Flow

```
Google Sign-In → JWT credential → Frontend decodes payload → POST /api/v1/auth/google
→ AuthService verifies via GoogleIdTokenVerifier → Creates/updates User → Returns UserDTO
→ Frontend stores user in Zustand (auth-storage) → All API calls include X-User-Id header
```

`X-User-Id` header is trusted without server-side session validation (MVP shortcut). The header defaults to `0` if missing.

## Key Conventions

### Backend
- All REST endpoints under `/api/v1/`
- Controllers use `@RestController` with `@RequestMapping`
- User identification via `X-User-Id` header (set by frontend from auth store)
- Entities use `@Builder` (Lombok) pattern; DTOs use `@Data @Builder`
- Repositories extend `JpaRepository` with `findByUserId` methods
- No Flyway; Hibernate `ddl-auto: update` auto-generates schema
- Test patterns: `@WebMvcTest` + `@MockitoBean` for controllers, `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` for services
- Nested `@Nested` test classes for logical grouping
- JaCoCo enforces 80% line coverage; Spotless enforces Palantir Java Format

### Frontend
- Feature-based folders: `features/auth`, `features/assessment`, `features/dashboard`
- API calls via native `fetch` in `src/services/api.js` — no Axios
- Assessment API layer in `src/features/assessment/services/assessmentApi.js` — 30+ functions with DTO mappers
- Auth token stored in localStorage via Zustand persist (`auth-storage`)
- Protected routes via `ProtectedRoute` HOC
- Tailwind CSS 4 via `@theme` directive in `src/styles/index.css` — no `tailwind.config.js`
- Theme switching via `data-theme` attribute on `<html>` (light/dark)
- Brand color: `#0ab842` (green); display font: Inter
- `clsx` + `tailwind-merge` for conditional class merging
- Icons: lucide-react; Toasts: react-hot-toast
- PWA enabled via vite-plugin-pwa (auto-update, installable)

### Environment Variables

**Frontend (.env):**
- `VITE_GOOGLE_CLIENT_ID` — Google OAuth client ID
- `VITE_AUTH_REQUIRED` — toggle auth enforcement (true/false)

**Backend (application.yml / env):**
- `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` — DB connection
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` — Bedrock access
- `GOOGLE_CLIENT_ID` — OAuth verification

## Specs & Docs

Business and architecture specs live in `specs/` — read these before making changes to assessment flow or API contracts. Each step has a dedicated spec (`step1_personal_risk_profile.md` through `step6_tax_optimization.md`) plus cross-cutting specs (`api_specification.md`, `database_design.md`, `backend_architecture.md`, `frontend_specification.md`).

Swagger UI available at `http://localhost:8081/swagger-ui.html` when backend is running.

## gstack

Use /browse from gstack for all web browsing. Never use mcp__claude-in-chrome__* tools.
Available skills: /office-hours, /plan-ceo-review, /plan-eng-review, /plan-design-review,
/design-consultation, /design-shotgun, /review, /ship, /land-and-deploy, /canary, /benchmark, /browse,
/connect-chrome, /qa, /qa-only, /design-review, /setup-browser-cookies, /setup-deploy, /retro,
/investigate, /document-release, /codex, /cso, /autoplan, /careful, /freeze, /guard,
/unfreeze, /gstack-upgrade.
