# Step 3: Assets & Liabilities

## Purpose
Capture the user's net worth (Assets − Liabilities) and current asset allocation to assess financial health, calculate the Debt-to-Income (DTI) ratio, and identify rebalancing opportunities.

## User Story
> *As a user, I want to list my assets and liabilities so that the system can calculate my net worth, evaluate my asset allocation against my risk profile, and check my debt burden.*

## Business Rules
| Rule | Detail |
|---|---|
| Asset classes | Savings & Investments, Real Assets, Alternative Investments. Further mapped into: Equity, Debt, Real Estate, Gold. |
| Liability types | Home Loan, Personal Loan, Business Loan, Credit Card Debt, Vehicle Loan, Gold Loan, Education Loan, Loan Against Property, Other. |
| Net worth | `Sum(Assets) − Sum(Liabilities)` (Displayed with smart formatting: Crores, Lakhs, etc.) |
| Asset Allocation | Auto-calculated per asset category. Compared against target moderate risk (Equity 50%, Debt 30%, Real Estate 15%, Gold 5%). Flags under-exposure in Equity (< 30%) or over-exposure in Real Estate (> 35%). |
| Debt-to-Income (DTI) ratio | `(monthlyEmiTotal / totalMonthlyIncome) * 100`. Flags: < 30% Healthy, 30-40% Monitor, > 40% Critical (alert). |
| Average Interest Rate | Weighted average of liability interest rates based on outstanding amounts. |

## Data Model

### Assets
| Field | Type | Constraints / Options |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `category` | VARCHAR | Primary category group |
| `subCategory` | VARCHAR | Specific instrument (e.g., Mutual Funds — Equity, EPF, Gold) |
| `name` | VARCHAR | User-provided or defaults to subCategory |
| `amount` | NUMBER | Current Value (> 0) |
| `purchaseValue` | NUMBER | Original Investment Value |
| `timeHorizon` | VARCHAR | Intended duration for holding asset |
| `liquidity` | VARCHAR | Detail on how quickly the asset can be liquidated |

### Liabilities
| Field | Type | Constraints / Options |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `category` | VARCHAR | Liability type |
| `name` | VARCHAR | User-provided or defaults to category |
| `amount` | NUMBER | Outstanding Principal (> 0) |
| `emi` | NUMBER | Monthly Payment |
| `interestRate` | NUMBER | Annual Interest Rate (%) |
| `monthsLeft` | NUMBER | Remaining Tenure |
| `moratoriumMonths` | NUMBER | Exists exclusively if category is 'Education Loan' |

## API Endpoints
- Triggers `addAssetApi` or `addLiabilityApi` (falls back to local Storage via Zustand store if API is offline)

## Acceptance Criteria
- [ ] User can add multiple assets and liabilities using dynamic form modals
- [ ] Asset modal adapts specific fields (e.g., purchase value, liquidity) based on selection
- [ ] Net worth computed and displayed in real-time with big formatting (Lakhs/Crores)
- [ ] Asset allocation pie chart (conic gradient) rendered and compared to target baselines
- [ ] DTI Ratio gauge rendered with progressive coloring (green to red) based on health limits
- [ ] Education Loan exposes special `moratoriumMonths` field

