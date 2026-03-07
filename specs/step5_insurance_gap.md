# Step 5: Insurance Gap Analysis

## Purpose
Evaluate existing life and health insurance coverage against calculated recommended levels. Uses a comprehensive Present Value (PV) approach for life coverage and family size scaling for health coverage, identifying precise gaps.

## User Story
> *As a user, I want to see if my insurance coverage is adequate to protect my family's living standard, goals, and liabilities, so I know exactly what additional policies to consider.*

## Business Rules
| Rule | Detail |
|---|---|
| Constants | Fixed Real Rate (r) = 1.887%, Life Expectancy (n) = 90 |
| Living Expenses Cover | `PV = Annual Expenses * [1 - (1+r)^-yearsRemaining] / r` where `yearsRemaining = max(1, 90 - partnerAge)` |
| Goals Cover | PV of goal gaps discounted to today. `Gap / (1+r)^horizon` |
| Liabilities Cover | Outstanding sum of all liabilities |
| Deductible Liquid Assets | Sum of liquid assets (e.g., Savings, FDs, Mutual Funds, Stocks, Equity) |
| Recommended Life Cover | `Max(0, Living Exp + Goals + Liabilities - Liquid Assets)` |
| Recommended Health Cover | Base ₹10 Lakhs. Multipliers based on family size (2: 1.2x, 3: 1.3x, 4: 1.5x, 5+: 1.7x) |
| Actual Covers | Corporate Life/Health + Personal Life (Sum Assured) + Personal Health (Sum Insured) |
| Gap severity | Shows progress bar `% covered`. Gap flag if Recommended > Actual. |

## Data Model

### Life Insurance
| Field | Type | Constraints |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `type` | VARCHAR | e.g. 'Term Life' |
| `sumAssured` | NUMBER | Amount covered |
| `premium` | NUMBER | Annual cost |
| `spouseAge` | NUMBER | Used to determine partner age for years remaining |

### Health Insurance
| Field | Type | Constraints |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `type` | VARCHAR | e.g. 'Family Floater' |
| `sumInsured` | NUMBER | Amount covered |
| `premium` | NUMBER | Annual cost |
| `copay` | NUMBER | % Co-pay if applicable |

## API Endpoints
- Triggers `saveInsuranceApi(lifeCover, healthCover)` (falls back to Zustand store)

## Acceptance Criteria
- [ ] PV formula calculates required life cover dynamically using user/spouse age, goals, and expenses
- [ ] Health cover scaled precisely based on family size and dependents input from Step 1
- [ ] Interactive modal allows detailing existing personal health and life policies
- [ ] Gaps are rendered visually emphasizing shortfalls in Lakhs/Crores format
