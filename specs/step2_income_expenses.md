# Step 2: Income & Expenses

## Purpose
Capture recurring income sources and expense categories to compute monthly cash flow and savings rate — the foundation for all financial planning.

## User Story
> *As a user, I want to enter my income sources and expenses so that the system can calculate my monthly surplus and savings rate.*

## Business Rules
| Rule | Detail |
|---|---|
| Frequency normalization | Calculates all amounts to monthly equivalents: `Monthly = amount`, `Quarterly = amount / 3`, `Yearly / One-time = amount / 12`. |
| Income TDS Deductions | Captures if tax is already deducted at source (`taxDeducted`) and allows user to input `tdsPercentage` (defaults to 10%). |
| Cash Flow Math | `surplus = totalMonthlyIncome - totalMonthlyExpenses`. `savingsRate = (surplus / totalMonthlyIncome) * 100`. |
| EMI Breakout | Expenses categorized as "EMIs (loan payments)" (or containing "EMI") are broken out separately from other expenses for visual cash flow representation. |
| Savings Health & Advice | If `savingsRate >= 20%`: Excellent status. If `< 20%`: Warning status, highlights top 3 discretionary expenses, and calculates `hypotheticalSavingsRate` if discretionary expenses were cut by 30%. |
| Step Validation | User must add at least one income source to proceed (throws error otherwise). If no expenses are added, shows a tip toast but allows progression. |

## Data Model

### Incomes (Income Item)
| Field | Type | Constraints / Options |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `category` | VARCHAR | Options: Salary, Business, Rental, Dividend, etc. |
| `source` | VARCHAR | Maps to `category` |
| `amount` | NUMBER | > 0 |
| `frequency` | VARCHAR | `Monthly`, `Quarterly`, `Yearly`, `One-time` |
| `taxDeducted` | BOOLEAN | Indicates if TDS is deducted |
| `tdsPercentage` | NUMBER | percentage (e.g. 10) |

### Expenses (Expense Item)
| Field | Type | Constraints / Options |
|---|---|---|
| `id` | NUMBER | Generated via `Date.now()` |
| `category` | VARCHAR | Options: Rent/Mortgage, EMIs, Utilities, Food & Groceries, etc. |
| `amount` | NUMBER | > 0 |
| `frequency` | VARCHAR | `Monthly`, `Quarterly`, `Yearly`, `One-time` |
| `type` | VARCHAR | `Essential` or `Discretionary` toggle |

## API Endpoints
- Triggers `addIncomeApi` or `addExpenseApi` (falls back to local Storage via Zustand store if API is offline)

## Acceptance Criteria
- [ ] User can add multiple income sources and expenses using modal popups
- [ ] Both modals use standard dropdowns for categories and frequency
- [ ] Expense modal includes Essential/Discretionary toggle
- [ ] Income modal includes Tax Deducted slider and TDS % input
- [ ] Monthly cash flow summary displays in real-time with EMI broken out
- [ ] Savings rate calculated and displayed with appropriate Warning/Excellent feedback
- [ ] Low savings rates trigger the 30% discretionary reduction hypothetical logic
- [ ] Progress blocked if Income is 0

