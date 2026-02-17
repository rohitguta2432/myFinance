# Step 2: Income & Expenses

## Purpose
Capture recurring income sources and expense categories to compute monthly cash flow and savings rate — the foundation for all financial planning.

## User Story
> *As a user, I want to enter my income sources and expenses so that the system can calculate my monthly surplus and savings rate.*

## Business Rules
| Rule | Detail |
|---|---|
| Frequency normalization | All amounts normalized to monthly for comparison |
| Savings rate | `(Total Income − Total Expenses) / Total Income × 100` |
| Health thresholds | < 20% savings → Warning (red), 20–50% → Good (yellow), > 50% → Excellent (green) |
| Essential vs discretionary | Expenses tagged as essential are excluded from optimization suggestions |
| At least one income | User cannot proceed without at least one income source |

## Data Model

### Incomes
| Field | Type | Constraints |
|---|---|---|
| `source_name` | VARCHAR(100) | NOT NULL |
| `amount` | DECIMAL(15,2) | NOT NULL |
| `frequency` | VARCHAR(20) | `MONTHLY`, `YEARLY`, `ONE_TIME` |

### Expenses
| Field | Type | Constraints |
|---|---|---|
| `category` | VARCHAR(100) | NOT NULL |
| `amount` | DECIMAL(15,2) | NOT NULL |
| `frequency` | VARCHAR(20) | Default `MONTHLY` |
| `is_essential` | BOOLEAN | Default TRUE |

## API Endpoints
- `GET /api/v1/assessment/financials` — Retrieve all incomes and expenses
- `POST /api/v1/assessment/income` — Add income source
- `POST /api/v1/assessment/expense` — Add expense entry

## Acceptance Criteria
- [ ] User can add multiple income sources and expenses
- [ ] Monthly cash flow summary displays in real-time
- [ ] Savings rate calculated and displayed with color-coded health indicator
- [ ] Data persisted to `incomes` and `expenses` tables
