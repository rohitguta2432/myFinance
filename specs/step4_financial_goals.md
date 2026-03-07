# Step 4: Financial Goals

## Purpose
Help users define future financial goals with inflation-adjusted target amounts and time horizons. The system calculates the required monthly investments (SIP) needed to reach these goals, considering current savings and comparing them against the user's calculated monthly surplus from Step 2.

## User Story
> *As a user, I want to set financial goals (retirement, home, education) so that the system can tell me how much I need to invest monthly to reach them and whether my current cash flow supports these ambitions.*

## Business Rules
| Rule | Detail |
|---|---|
| Goal types | Home Purchase, Child Education, Child Marriage, Retirement, Emergency, Business, Vehicle, Custom |
| Importance levels | Critical, High, Medium, Low |
| Inflation adjustment | `Future Cost = Current Cost × (1 + inflation_rate)^years`; Default inflation is 6% p.a. |
| Target Buffer | **Buffered Cost = Future Cost × 1.20** (a 20% buffer is added to ensure safety margin) |
| Current Savings Growth | `Savings Growth = Current Savings × (1 + expected_return)^years` |
| Goal Gap | `Gap to Fill = Max(0, Buffered Cost - Savings Growth)` |
| Expected returns | Assumed **12%** return rate for SIP and Savings Growth calculations |
| SIP calculation | `Monthly SIP = Gap × r / ((1 + r)^n − 1)` where `r = expected_return / 12` |
| Monthly Surplus integration| Computes `Total Monthly Surplus = Total Monthly Income - (Total Monthly Expenses + EMI)`. Determines if all goals are achievable (`Total SIP <= Surplus`) and calculates feasibility buffer or shortfall. |

## Data Model
| Field | Type | Constraints / Options |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `type` | VARCHAR | e.g., 'home', 'education', 'retirement' |
| `name` | VARCHAR | User-provided or defaults to type label |
| `cost` | NUMBER | Current target cost |
| `horizon` | NUMBER | Time horizon in years |
| `currentSavings` | NUMBER | Amount already saved towards this goal |
| `inflation` | NUMBER | Expected inflation rate (Default 6%) |
| `importance` | VARCHAR | 'Critical', 'High', 'Medium', 'Low' |

## API Endpoints
- Triggers `addGoalApi` (falls back to local Storage via Zustand store if API is offline)

## Acceptance Criteria
- [ ] User can add, edit, and delete multiple goals (up to UI limits, default max limit check observed in frontend)
- [ ] Future value computed with a 12% returns assumption and 20% target buffer
- [ ] Monthly SIP requirement mathematically derived and displayed
- [ ] Shows SIP shortfall and visually tracks progress using a progress bar
- [ ] Computes 'All Goals Summary' including achievable status by comparing total required SIP to real-time monthly surplus
