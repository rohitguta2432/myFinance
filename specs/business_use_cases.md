# Business Use Cases — MyFinance Assessment Wizard

This document describes the business use case for each of the 6 screens in the MyFinance financial assessment wizard. Each screen captures specific financial data points that feed into downstream advisory calculations.

---

## Screen 1: Personal Risk Profile

### Purpose
Capture demographic and risk-related information to classify the user's investment risk tolerance and personalize all subsequent financial recommendations.

### User Story
> *As a user, I want to answer a few questions about my profile and risk appetite so that the system can recommend an asset allocation suited to my situation.*

### Business Rules
| Rule | Detail |
|---|---|
| Age validation | Must be ≥ 18 |
| City tier mapping | Metro / Tier 1 / Tier 2 / Tier 3 — affects cost-of-living assumptions |
| Risk score calculation | Weighted scoring of 5 risk questions (0–10 scale) |
| Risk tolerance bands | 0–3.33 → **Conservative**, 3.34–6.66 → **Moderate**, 6.67–10 → **Aggressive** |
| Employment impact | Self-employed users get a slightly more conservative adjustment |

### Data Captured
`age`, `city_tier`, `marital_status`, `dependents`, `employment_type`, `residency_status`, `risk_score`, `risk_tolerance`

### Acceptance Criteria
- [ ] User can fill all fields and proceed to Step 2
- [ ] Risk tolerance is auto-calculated from the 5 risk questions
- [ ] Profile is persisted to `profiles` table via `POST /api/v1/assessment/profile`
- [ ] Returning users see pre-populated data via `GET /api/v1/assessment/profile`

---

## Screen 2: Income & Expenses

### Purpose
Capture recurring income sources and expense categories to compute monthly cash flow and savings rate — the foundation for all financial planning.

### User Story
> *As a user, I want to enter my income sources and expenses so that the system can calculate my monthly surplus and savings rate.*

### Business Rules
| Rule | Detail |
|---|---|
| Frequency normalization | All amounts normalized to monthly for comparison |
| Savings rate | `(Total Income − Total Expenses) / Total Income × 100` |
| Health thresholds | < 20% savings → Warning (red), 20–50% → Good (yellow), > 50% → Excellent (green) |
| Essential vs discretionary | Expenses tagged as essential are excluded from optimization suggestions |
| At least one income | User cannot proceed without at least one income source |

### Data Captured
- **Incomes**: `source_name`, `amount`, `frequency` (Monthly / Yearly / One-time)
- **Expenses**: `category`, `amount`, `frequency`, `is_essential`

### Acceptance Criteria
- [ ] User can add multiple income sources and expenses
- [ ] Monthly cash flow summary displays in real-time
- [ ] Data persisted via `POST /api/v1/assessment/income` and `POST /api/v1/assessment/expense`
- [ ] Savings rate calculated and displayed with color-coded health indicator

---

## Screen 3: Assets & Liabilities

### Purpose
Capture the user's net worth (Assets − Liabilities) and current asset allocation to assess financial health and identify rebalancing opportunities.

### User Story
> *As a user, I want to list my assets and liabilities so that the system can calculate my net worth and assess my asset allocation.*

### Business Rules
| Rule | Detail |
|---|---|
| Asset categories | Real Estate, Equity, Mutual Funds, Gold, Fixed Deposits, Cash/Savings |
| Liability types | Home Loan, Car Loan, Personal Loan, Education Loan, Credit Card |
| Net worth | `Sum(Assets) − Sum(Liabilities)` |
| Allocation % | Auto-calculated per asset category as percentage of total assets |
| Debt-to-asset ratio | If > 50%, flag as high financial risk |

### Data Captured
- **Assets**: `asset_type`, `name`, `current_value`
- **Liabilities**: `liability_type`, `lender_name`, `outstanding_amount`, `monthly_emi`, `interest_rate`

### Acceptance Criteria
- [ ] User can add multiple assets and liabilities
- [ ] Net worth computed and displayed in real-time
- [ ] Asset allocation pie chart rendered
- [ ] Data persisted via `POST /api/v1/assessment/asset` and `POST /api/v1/assessment/liability`

---

## Screen 4: Financial Goals

### Purpose
Help users define future financial goals with inflation-adjusted target amounts and time horizons, enabling the system to compute required monthly investments (SIP).

### User Story
> *As a user, I want to set financial goals (retirement, home, education) so that the system can tell me how much I need to invest monthly to reach them.*

### Business Rules
| Rule | Detail |
|---|---|
| Goal types | Retirement, Home Purchase, Car, Education, Marriage, Emergency Fund, Travel, Custom |
| Inflation adjustment | `Future Value = Current Cost × (1 + inflation_rate)^years` |
| Default inflation | 6% p.a. (India average) |
| SIP calculation | `Monthly SIP = FV × r / ((1 + r)^n − 1)` where `r = expected_return/12` |
| Expected returns | Conservative: 8%, Moderate: 12%, Aggressive: 15% (based on risk profile) |

### Data Captured
`goal_type`, `goal_name`, `target_amount`, `current_cost`, `time_horizon_years`, `inflation_rate`

### Acceptance Criteria
- [ ] User can add multiple goals with different types
- [ ] Inflation-adjusted future value computed and shown
- [ ] Monthly SIP requirement displayed per goal
- [ ] Data persisted via `POST /api/v1/assessment/goal`

---

## Screen 5: Insurance Gap Analysis

### Purpose
Evaluate existing life and health insurance coverage against recommended levels using the Human Life Value (HLV) method, and identify coverage gaps.

### User Story
> *As a user, I want to see if my insurance coverage is adequate so that I know if my family is financially protected.*

### Business Rules
| Rule | Detail |
|---|---|
| Life insurance HLV | `Annual Income × Remaining Working Years × (1 − Tax Rate)` |
| Recommended life cover | 10–15× annual income (simplified) |
| Health insurance | Minimum ₹5L individual, ₹10L family (India market standard) |
| Coverage gap | `Recommended − Existing Coverage` |
| Gap severity | < 50% covered → Critical (red), 50–80% → Moderate (yellow), > 80% → Adequate (green) |

### Data Captured
- **Life Insurance**: `insurance_type=LIFE`, `policy_name`, `coverage_amount`, `premium_amount`
- **Health Insurance**: `insurance_type=HEALTH`, `policy_name`, `coverage_amount`, `premium_amount`

### Acceptance Criteria
- [ ] Recommended coverage is auto-calculated from income data (Step 2)
- [ ] Gap meter visualization shows current vs recommended
- [ ] Users can enter existing policy details
- [ ] Data persisted via `POST /api/v1/assessment/insurance`

---

## Screen 6: Tax Optimization

### Purpose
Compare the Old and New income tax regimes (India) based on the user's income and declared deductions, and recommend the optimal regime.

### User Story
> *As a user, I want to compare my tax liability under the Old and New tax regimes so that I can choose the one that saves me the most money.*

### Business Rules
| Rule | Detail |
|---|---|
| Section 80C limit | ₹1,50,000 p.a. (EPF/VPF, PPF/ELSS, tuition fees, LIC, home loan principal) |
| Section 80D limit | ₹25,000 self + ₹25,000 parents (₹50,000 if senior citizen) |
| Old regime slabs | 0–2.5L: 0%, 2.5–5L: 5%, 5–10L: 20%, >10L: 30% |
| New regime slabs (FY 2024-25) | 0–3L: 0%, 3–7L: 5%, 7–10L: 10%, 10–12L: 15%, 12–15L: 20%, >15L: 30% |
| Standard deduction | ₹50,000 (Old regime), ₹75,000 (New regime FY 2024-25) |
| Regime recommendation | Compare calculated tax under both regimes → suggest the lower one |

### Data Captured
`selected_regime`, `ppf_elss_amount`, `epf_vpf_amount`, `tuition_fees_amount`, `lic_premium_amount`, `home_loan_principal`, `health_insurance_premium`, `parents_health_insurance`

### Acceptance Criteria
- [ ] Both regime calculations are shown side-by-side
- [ ] Recommended regime is highlighted with savings amount
- [ ] 80C components are individually editable with running total (cap at ₹1.5L)
- [ ] Data persisted via `POST /api/v1/assessment/tax`
- [ ] User can switch selected regime and see recalculation
