# Step 3: Assets & Liabilities

## Purpose
Capture the user's net worth (Assets − Liabilities) and current asset allocation to assess financial health and identify rebalancing opportunities.

## User Story
> *As a user, I want to list my assets and liabilities so that the system can calculate my net worth and assess my asset allocation.*

## Business Rules
| Rule | Detail |
|---|---|
| Asset categories | Real Estate, Equity, Mutual Funds, Gold, Fixed Deposits, Cash/Savings |
| Liability types | Home Loan, Car Loan, Personal Loan, Education Loan, Credit Card |
| Net worth | `Sum(Assets) − Sum(Liabilities)` |
| Allocation % | Auto-calculated per asset category as percentage of total assets |
| Debt-to-asset ratio | If > 50%, flag as high financial risk |

## Data Model

### Assets
| Field | Type | Constraints |
|---|---|---|
| `asset_type` | VARCHAR(50) | NOT NULL |
| `name` | VARCHAR(100) | — |
| `current_value` | DECIMAL(15,2) | NOT NULL |
| `allocation_percentage` | DECIMAL(5,2) | Derived |

### Liabilities
| Field | Type | Constraints |
|---|---|---|
| `liability_type` | VARCHAR(50) | NOT NULL |
| `name` | VARCHAR(100) | — |
| `outstanding_amount` | DECIMAL(15,2) | NOT NULL |
| `monthly_emi` | DECIMAL(15,2) | — |
| `interest_rate` | DECIMAL(5,2) | — |

## API Endpoints
- `GET /api/v1/assessment/balance-sheet` — Retrieve all assets and liabilities
- `POST /api/v1/assessment/asset` — Add asset entry
- `POST /api/v1/assessment/liability` — Add liability entry

## Acceptance Criteria
- [ ] User can add multiple assets and liabilities
- [ ] Net worth computed and displayed in real-time
- [ ] Asset allocation pie chart rendered
- [ ] Debt-to-asset ratio flagged when > 50%
