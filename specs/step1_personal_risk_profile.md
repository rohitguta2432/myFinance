# Step 1: Personal Risk Profile

## Purpose
Capture demographic and risk-related information to classify the user's investment risk tolerance and personalize all subsequent financial recommendations.

## User Story
> *As a user, I want to answer a few questions about my profile and risk appetite so that the system can recommend an asset allocation suited to my situation.*

## Business Rules
| Rule | Detail |
|---|---|
| Age validation | Must be ≥ 18 |
| City tier mapping | Metro / Tier 1 / Tier 2 / Tier 3 — affects cost-of-living assumptions |
| Risk score calculation | Weighted scoring of 5 risk questions (0–10 scale) |
| Risk tolerance bands | 0–3.33 → **Conservative**, 3.34–6.66 → **Moderate**, 6.67–10 → **Aggressive** |
| Employment impact | Self-employed users get a slightly more conservative adjustment |

## Data Model
| Field | Type | Constraints |
|---|---|---|
| `age` | INTEGER | ≥ 18 |
| `city_tier` | VARCHAR(20) | `METRO`, `TIER_1`, `TIER_2`, `TIER_3` |
| `marital_status` | VARCHAR(20) | `SINGLE`, `MARRIED` |
| `dependents` | INTEGER | Default 0 |
| `employment_type` | VARCHAR(50) | `SALARIED`, `SELF_EMPLOYED` |
| `residency_status` | VARCHAR(50) | `RESIDENT`, `NRI` |
| `risk_score` | DECIMAL(4,2) | 0.00–10.00 (server-calculated) |
| `risk_tolerance` | VARCHAR(20) | `CONSERVATIVE`, `MODERATE`, `AGGRESSIVE` |

## API Endpoints
- `GET /api/v1/assessment/profile` — Retrieve saved profile
- `POST /api/v1/assessment/profile` — Create or update profile

## Acceptance Criteria
- [ ] User can fill all fields and proceed to Step 2
- [ ] Risk tolerance is auto-calculated from the 5 risk questions
- [ ] Profile is persisted to `profiles` table
- [ ] Returning users see pre-populated data
