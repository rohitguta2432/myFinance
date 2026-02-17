# Step 6: Tax Optimization

## Purpose
Compare the Old and New income tax regimes (India) based on the user's income and declared deductions, and recommend the optimal regime.

## User Story
> *As a user, I want to compare my tax liability under the Old and New tax regimes so that I can choose the one that saves me the most money.*

## Business Rules
| Rule | Detail |
|---|---|
| Section 80C limit | ₹1,50,000 p.a. (EPF/VPF, PPF/ELSS, tuition fees, LIC, home loan principal) |
| Section 80D limit | ₹25,000 self + ₹25,000 parents (₹50,000 if senior citizen) |
| Old regime slabs | 0–2.5L: 0%, 2.5–5L: 5%, 5–10L: 20%, >10L: 30% |
| New regime slabs (FY 2024-25) | 0–3L: 0%, 3–7L: 5%, 7–10L: 10%, 10–12L: 15%, 12–15L: 20%, >15L: 30% |
| Standard deduction | ₹50,000 (Old regime), ₹75,000 (New regime FY 2024-25) |
| Regime recommendation | Compare calculated tax under both → suggest the lower one |

## Data Model
| Field | Type | Constraints |
|---|---|---|
| `selected_regime` | VARCHAR(10) | `OLD`, `NEW` |
| `epf_vpf_amount` | DECIMAL(15,2) | Default 0 |
| `ppf_elss_amount` | DECIMAL(15,2) | Default 0 |
| `tuition_fees_amount` | DECIMAL(15,2) | Default 0 |
| `lic_premium_amount` | DECIMAL(15,2) | Default 0 |
| `home_loan_principal` | DECIMAL(15,2) | Default 0 |
| `health_insurance_premium` | DECIMAL(15,2) | Default 0 |
| `parents_health_insurance` | DECIMAL(15,2) | Default 0 |
| `calculated_tax_old` | DECIMAL(15,2) | Server-calculated |
| `calculated_tax_new` | DECIMAL(15,2) | Server-calculated |

## API Endpoints
- `GET /api/v1/assessment/tax` — Retrieve tax planning data
- `POST /api/v1/assessment/tax` — Create or update tax data

## Acceptance Criteria
- [ ] Both regime calculations shown side-by-side
- [ ] Recommended regime highlighted with savings amount
- [ ] 80C components individually editable with running total (cap at ₹1.5L)
- [ ] User can switch selected regime and see recalculation
- [ ] Data persisted to `tax_planning` table
