# Step 1: Personal Risk Profile

## Purpose
Capture demographic and risk-related information to classify the user's investment risk tolerance and personalize all subsequent financial recommendations.

## User Story
> *As a user, I want to answer a few questions about my profile and risk appetite so that the system can recommend an asset allocation suited to my situation.*

## Business Rules
| Rule | Detail |
|---|---|
| Age validation | Slider ranges from 18 to 75+. Mandatory minimum age is 18 to proceed. |
| Dependents Logic | Tracks total `dependents`. If `dependents ≥ 1`, conditionally asks for `childDependents` (under 18). `childDependents` cannot exceed `dependents`. |
| Risk score calculation | 5 multiple-choice questions (options scored 1-3). Raw score (5-15) is scaled to a 0-10 mapped score: `Math.round(((rawRiskScore - 5) / 10) * 10)`. |
| Risk tolerance bands | `mappedScore <= 3` → **Conservative**, `mappedScore <= 6` → **Moderate**, `mappedScore > 6` → **Aggressive**. |
| Asset Allocation | **Conservative**: Equity 20%, Debt 60%, Gold 10%, REITs 10%<br>**Moderate**: Equity 50%, Debt 30%, Gold 5%, REITs 15%<br>**Aggressive**: Equity 70%, Debt 15%, Gold 5%, REITs 10% |

## Data Model
| Field | Type | Constraints / Options |
|---|---|---|
| `age` | INTEGER | ≥ 18 (Max 75+ via UI slider) |
| `city` | VARCHAR | Free text input |
| `maritalStatus` | VARCHAR | `single`, `married`, `divorced`, `widowed` |
| `dependents` | INTEGER | ≥ 0 (Counter UI) |
| `childDependents` | INTEGER | ≥ 0, ≤ `dependents` (Conditionally shown if dependents ≥ 1) |
| `employmentType` | VARCHAR | `Salaried`, `Self-Employed`, `Business`, `Retired`, `Unemployed` |
| `residencyStatus`| VARCHAR | `Resident`, `NRI`, `OCI` |
| `riskAnswers` | JSON | Key-value mapper for 5 questions (1-3 score each) |
| `riskTolerance` | VARCHAR | `conservative`, `moderate`, `aggressive` (Calculated on frontend) |

## API Endpoints
- (Uses Next.js / Supabase setup, or local storage depending on implementation phase)
- Currently triggers `saveProfileApi` (falling back to LocalStorage persist if API fails)

## Acceptance Criteria
- [ ] User can fill all fields and proceed to Step 2
- [ ] Next button is disabled until all mandatory fields (including 5 risk questions) are answered
- [ ] Child dependents UI logically follows general dependents UI
- [ ] Risk tolerance is auto-calculated and displayed with the corresponding theme color and asset breakdown
- [ ] Profile state is saved via `useAssessmentStore` and persisted

