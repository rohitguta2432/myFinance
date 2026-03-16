# myFinance — Project Context

**One-line**: Indian personal finance advisor — 6-step wizard → premium dashboard with tax planning, insurance gaps, and AI chat.

## Stack
- **Frontend**: React 19 + Vite 7 + Tailwind CSS v4 + Zustand (persist) + React Router 7 + Lucide icons
- **Backend**: Spring Boot 3.4.1 + Java 21 + Lombok + Spring Data JPA + Bean Validation
- **AI**: AWS Bedrock (`amazon.nova-lite-v1:0`) via `BedrockChatService`
- **DB**: H2 in-memory (dev) → PostgreSQL 14 (prod via Docker)
- **Deploy**: Docker Compose (3 services) → AWS EC2 via rsync

## Key Architecture
- **State**: Zustand store with `persist` middleware → `localStorage` key `assessment-storage`
- **HTTP**: Native `fetch` wrapper in `src/services/api.js` — zero external HTTP libs
- **Styling**: Tailwind v4 + `clsx` + `tailwind-merge` — no vanilla CSS
- **API base**: `/api/v1/assessment` on port 8081 (mapped to 8080 in Docker)
- **Backend pattern**: Controller → Service → JpaRepository (2 controllers, 2 services, 8 repos)
- **DTOs**: Lombok `@Data @Builder` — NOT Java records

## Project Structure
```
src/features/assessment/   ← 6 wizard step pages + Zustand store
src/features/dashboard/    ← Financial dashboard tabs (tax, insurance, action plan)
src/hooks/                 ← 11 domain hooks (useTaxAnalysis, useFinancialHealthScore, etc.)
src/components/            ← Shared UI (AiChatWidget, Layout, ThemeToggle)
src/services/api.js        ← Fetch wrapper
src/constants/enums.js     ← Shared enums (CITY_TIERS, TAX_REGIME, FREQUENCY, etc.)
backend/                   ← Spring Boot app
```

## Current Gaps
- ❌ No authentication (no Spring Security)
- ❌ No tests (no Vitest or JUnit tests written)
- ❌ No caching (no @Cacheable)
- ⚠️ Docker Compose has hardcoded DB password
