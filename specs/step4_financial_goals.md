# Step 4: Financial Goals

## Purpose
Help users define future financial goals with inflation-adjusted target amounts and time horizons, enabling the system to compute required monthly investments (SIP).

## User Story
> *As a user, I want to set financial goals (retirement, home, education) so that the system can tell me how much I need to invest monthly to reach them.*

## Business Rules
| Rule | Detail |
|---|---|
| Goal types | Retirement, Home Purchase, Car, Education, Marriage, Emergency Fund, Travel, Custom |
| Inflation adjustment | `Future Value = Current Cost × (1 + inflation_rate)^years` |
| Default inflation | 6% p.a. (India average) |
| SIP calculation | `Monthly SIP = FV × r / ((1 + r)^n − 1)` where `r = expected_return/12` |
| Expected returns | Conservative: 8%, Moderate: 12%, Aggressive: 15% (linked to risk profile from Step 1) |

## Data Model
| Field | Type | Constraints |
|---|---|---|
| `goal_type` | VARCHAR(50) | NOT NULL |
| `name` | VARCHAR(100) | — |
| `target_amount` | DECIMAL(15,2) | NOT NULL |
| `current_cost` | DECIMAL(15,2) | — |
| `time_horizon_years` | INTEGER | NOT NULL |
| `inflation_rate` | DECIMAL(4,2) | Default 6.00 |

## API Endpoints
- `GET /api/v1/assessment/goals` — Retrieve all financial goals
- `POST /api/v1/assessment/goal` — Add a financial goal

## Acceptance Criteria
- [ ] User can add multiple goals with different types
- [ ] Inflation-adjusted future value computed and shown
- [ ] Monthly SIP requirement displayed per goal
- [ ] Data persisted to `financial_goals` table
