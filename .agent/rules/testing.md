# Testing Requirements

## Current State: No Tests Exist ⚠️

The myFinance project currently has **zero test files** in `src/` and no test framework installed.
This rule defines the **target state** when testing is adopted.

## Target Test Framework

| Layer | Framework | Status |
|-------|-----------|--------|
| Frontend: React components | Vitest + React Testing Library | ❌ Not installed |
| Frontend: Hooks (tax, insurance) | Vitest | ❌ Not installed |
| Backend: Spring Boot services | `spring-boot-starter-test` (JUnit 5 + Mockito) | ✅ In pom.xml |
| Backend: Integration tests | `@SpringBootTest` | ❌ No tests written |
| E2E | Playwright or Cypress | ❌ Not installed |

## When Tests Are Adopted — Follow TDD

MANDATORY workflow:
1. Write test first (RED)
2. Run test - it should FAIL
3. Write minimal implementation (GREEN)
4. Run test - it should PASS
5. Refactor (IMPROVE)
6. Target: 80% coverage

## Priority Test Targets

These are the **highest-value** areas to test first:

### 1. Financial Calculation Hooks (CRITICAL)
- `useTaxAnalysis.js` — Old vs New regime with known inputs/outputs
- `useFinancialHealthScore.js` — Score calculation edge cases
- `useInsuranceAnalysis.js` — Gap detection logic
- `useActionPlan.js` — Priority action generation

### 2. Zustand Store Operations
- `useAssessmentStore.js` — Add/remove/update for incomes, expenses, assets, liabilities, goals, insurance
- Persistence: Verify data survives Zustand persist cycle

### 3. Spring Boot Backend
- `AssessmentService` — CRUD operations
- `BedrockChatService` — AI chat request/response handling
- Repository queries — H2 and PostgreSQL compatibility

### 4. India-Specific Edge Cases
- Tax: ₹0 income, ₹7L boundary, ₹12L Section 87A rebate boundary, ₹24L+ slab
- Assets: Zero assets, single asset, mixed categories
- Insurance: No policies, corporate-only, mixed personal + corporate
