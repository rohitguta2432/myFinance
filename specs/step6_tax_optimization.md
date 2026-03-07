# Step 6: Tax Optimization

## Purpose
Compare the Old and New income tax regimes (India) for FY 2026-27 based on the user's extrapolated annual income and declared/auto-detected deductions to recommend the optimal regime.

## User Story
> *As a user, I want to compare my tax liability under the Old and New tax regimes with smart auto-filling of my investments so that I can confidently choose the one that saves me the most money.*

## Business Rules
| Rule | Detail |
|---|---|
| Auto-Income calculation | Annualizes incomes (Monthly × 12, Quarterly × 4, Yearly) from Step 2 |
| Section 80C | Cap: ₹1,50,000. Auto-fetches EPF, PPF, NPS, Life Insurance (from prior steps). Allows manual input of Home Loan Principal, Tuition Fees, NSC/FD |
| Section 80D | Self/Spouse Cap: ₹25,000. Parents <60 Cap: ₹25,000. Parents >60 Cap: ₹50,000. |
| Other Deductions | Additional NPS (Cap ₹50k), Home Loan Interest (Cap ₹2L), Education Loan Interest, Donations |
| HRA Auto-calculation | Computes `annualRentPaid` from Step 2 expenses. Assumes Basic = 50% Salary and HRA Received = 40% Basic. Exemption = Min(Actual HRA, 50% Basic, Rent - 10% Basic) |
| Old Regime Tax | Std. Deduction = ₹50,000. Slabs: 0–2.5L (0%), 2.5–5L (5%), 5–10L (20%), >10L (30%) |
| New Regime Tax | Std. Deduction = ₹75,000. Slabs: 0-3L (0%), 3-7L (5%), 7-10L (10%), 10-12L (15%), 12-15L (20%), >15L (30%). Rebate: Zero tax if taxable income <= 7L. |
| Health & Education Cess | 4% applied to the computed base tax in both regimes |
| Regime Recommendation | System recommends and auto-selects the regime yielding lower `Total Tax` |

## Data Model
| Field | Type | Constraints / Options |
|---|---|---|
| `taxRegime` | VARCHAR | 'old' or 'new' |
| `investments80C` | NUMBER | Total qualified 80C amount (capped at 1.5L) |

*Manual input deductions (80D, Other) are currently kept in frontend state to calculate the real-time tax comparison, saving only the final choices.*

## API Endpoints
- Triggers `saveTaxApi(taxRegime, investments80C)` (falls back to local Storage via Zustand if API is offline)

## Acceptance Criteria
- [ ] Incomes annualized automatically to generate Gross Total Income
- [ ] Deductions smart-mapped from Step 3 (Assets) and Step 5 (Insurance)
- [ ] HRA heuristically calculated based on rented property expense
- [ ] Both regime calculations shown side-by-side with 4% cess addition
- [ ] Recommended regime highlighted with precise savings amount prominently
- [ ] Prevent completion if no tax regime is manually or automatically selected
