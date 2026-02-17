# Step 5: Insurance Gap Analysis

## Purpose
Evaluate existing life and health insurance coverage against recommended levels using the Human Life Value (HLV) method, and identify coverage gaps.

## User Story
> *As a user, I want to see if my insurance coverage is adequate so that I know if my family is financially protected.*

## Business Rules
| Rule | Detail |
|---|---|
| Life insurance HLV | `Annual Income × Remaining Working Years × (1 − Tax Rate)` |
| Recommended life cover | 10–15× annual income (simplified rule of thumb) |
| Health insurance | Minimum ₹5L individual, ₹10L family (India market standard) |
| Coverage gap | `Recommended − Existing Coverage` |
| Gap severity | < 50% covered → Critical (red), 50–80% → Moderate (yellow), > 80% → Adequate (green) |

## Data Model
| Field | Type | Constraints |
|---|---|---|
| `insurance_type` | VARCHAR(50) | `LIFE`, `HEALTH` |
| `policy_name` | VARCHAR(100) | — |
| `coverage_amount` | DECIMAL(15,2) | NOT NULL |
| `premium_amount` | DECIMAL(15,2) | — |
| `renewal_date` | DATE | — |

## API Endpoints
- `GET /api/v1/assessment/insurance` — Retrieve all insurance entries
- `POST /api/v1/assessment/insurance` — Add a life or health insurance policy

## Acceptance Criteria
- [ ] Recommended coverage auto-calculated from income data (Step 2)
- [ ] Gap meter visualization shows current vs recommended
- [ ] Users can enter existing policy details
- [ ] Data persisted to `insurances` table
