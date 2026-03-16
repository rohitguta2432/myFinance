---
name: continuous-learning
description: Instinct-based learning system capturing myFinance coding patterns. Pre-seeded with verified instincts from actual codebase analysis.
origin: ECC (continuous-learning-v2, adapted)
version: 2.1.0
---

# Continuous Learning — myFinance Instincts

Pre-seeded instincts extracted from scanning the **actual myFinance codebase** (March 2026).

## Verified Instincts — From Actual Code

### State Management (Zustand)

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `zustand-persist-store` | When creating state stores | 0.95 | `useAssessmentStore.js` uses `create(persist(...))` with key `assessment-storage` |
| `zustand-immutable-set` | When updating store arrays | 0.95 | All store methods use `set((state) => ({ items: [...state.items, newItem] }))` |
| `zustand-remove-by-filter` | When deleting items | 0.95 | All remove methods use `.filter(i => i.id !== id)` pattern |
| `zustand-update-by-map` | When updating items | 0.90 | `updateIncome`, `updateExpense`, `updateGoal` use `.map(i => i.id === id ? {...i, ...updates} : i)` |

### Custom Hooks — Domain Logic

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `usememo-expensive-calcs` | When computing financial values | 0.95 | `useTaxAnalysis` wraps entire return in `useMemo` with store dependencies |
| `domain-hook-pattern` | When adding new financial analysis | 0.90 | 11 domain hooks in `src/hooks/`: tax, health score, insurance, benchmarks, time machine, etc. |
| `india-currency-fmt` | When formatting ₹ values | 0.90 | `fmt()` in `useTaxAnalysis.js`: Cr for ≥1Cr, L for ≥1L, else toLocaleString('en-IN') |
| `annual-frequency-calc` | When annualizing amounts | 0.85 | `calculateAnnual()`: Monthly×12, Quarterly×4, Yearly×1 |

### Styling (Tailwind CSS v4)

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `tailwind-v4-classes` | When adding CSS | 0.95 | `@tailwindcss/vite` v4.1.18 in devDependencies |
| `clsx-tailwind-merge` | When composing conditional classes | 0.90 | Both `clsx` and `tailwind-merge` in dependencies |
| `lucide-react-icons` | When adding icons | 0.85 | `lucide-react` v0.564.0 in dependencies |

### Architecture

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `feature-folder-structure` | When creating new features | 0.95 | `src/features/assessment/` and `src/features/dashboard/` with co-located pages, store |
| `shared-hooks-folder` | When adding reusable hooks | 0.90 | `src/hooks/` with 11 domain hooks shared across features |
| `stitch-raw-html-reference` | When implementing UI from designs | 0.85 | `src/stitch_raw_html/` contains 9 reference HTML files for each wizard step |
| `native-fetch-api` | When making API calls | 0.90 | `src/services/api.js` — custom `fetch` wrapper, zero HTTP dependencies |

### Backend (Spring Boot 3.4.1)

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `lombok-entities` | When creating Java classes | 0.90 | `@Data`, `@Builder` via Lombok in pom.xml |
| `h2-dev-pg-prod` | When configuring DB | 0.95 | H2 in application.yml, PostgreSQL via Docker Compose env vars |
| `controller-service-repo` | When adding backend features | 0.90 | 2 controllers, 2 services, 8 repositories — standard layered arch |
| `bedrock-ai-integration` | When working with AI chat | 0.85 | `BedrockChatService` uses AWS SDK `bedrockruntime` for `amazon.nova-lite-v1:0` |
| `assessment-api-base` | When adding API endpoints | 0.90 | All endpoints under `/api/v1/assessment` |

### Deployment

| Instinct | Trigger | Confidence | Evidence |
|----------|---------|------------|----------|
| `docker-compose-3-service` | When configuring deployment | 0.95 | `docker-compose.yml`: postgres + backend + frontend (3 services) |
| `ec2-rsync-deploy` | When deploying | 0.85 | `.agent/workflows/deploy.md` — rsync to EC2 |
| `port-mapping-8081-to-8080` | When configuring ports | 0.90 | Backend runs on 8081, Docker maps to 8080 |

## How to Use

1. **Reference during coding**: Check relevant instincts before implementing a pattern
2. **Add new instincts**: When a pattern is used 3+ times, document it here
3. **Increase confidence**: When pattern is reused successfully without correction
4. **Decrease confidence**: When pattern is explicitly corrected or abandoned

## Instinct Evolution

When 3+ related instincts cluster, create a dedicated SKILL.md:
- Example: If 5+ "zustand-*" instincts accumulate → create `zustand-patterns/SKILL.md`
- Example: If 5+ "india-tax-*" instincts accumulate → create `india-tax-engine/SKILL.md`

*Instinct-based learning: every pattern is captured, verified, and ready to reuse.*
