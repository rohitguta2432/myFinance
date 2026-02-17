# API Specification: MyFinance

## Overview
This document outlines the REST API endpoints for the MyFinance application. The API follows standard REST principles and uses JSON for data exchange.

## Base URL
`/api/v1`

## Authentication
All protected endpoints require a Bearer Token in the `Authorization` header.

## Endpoints

### 1. Authentication
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/auth/signup` | Register a new user |
| `POST` | `/auth/login` | Authenticate user and get token |

### 2. Assessment Flow (Wizard)

**Step 1: Profile & Risk**
-   `GET /assessment/profile` - Get existing profile data.
-   `POST /assessment/profile` - Save/Update profile and risk inputs.
    -   *Payload:* `{ age, city_tier, marital_status, dependents, risk_answers: [...] }`

**Step 2: Income & Expenses**
-   `GET /assessment/financials` - Get income and expense lists.
-   `POST /assessment/income` - Add an income source.
-   `POST /assessment/expense` - Add an expense.
-   `PUT /assessment/income/{id}` - Update income.
-   `PUT /assessment/expense/{id}` - Update expense.

**Step 3: Assets & Liabilities**
-   `GET /assessment/balance-sheet` - Get assets and liabilities.
-   `POST /assessment/asset` - Add an asset (Real Estate, Equity, etc.).
-   `POST /assessment/liability` - Add a liability (Loan).

**Step 4: Financial Goals**
-   `GET /assessment/goals` - Get all goals.
-   `POST /assessment/goal` - Add a financial goal.
    -   *Payload:* `{ type, name, target_amount, time_horizon }`

**Step 5: Insurance**
-   `GET /assessment/insurance` - Get insurance coverage.
-   `POST /assessment/insurance` - Add/Update insurance policies.

**Step 6: Tax Planning**
-   `GET /assessment/tax` - Get tax details and regime comparison.
-   `POST /assessment/tax` - Update tax investments (80C, 80D).

### 3. Dashboard
-   `GET /dashboard/summary` - Returns calculated Net Worth, Health Score, and key insights.

## Error Handling
Standard HTTP Status Codes:
-   `200 OK`: Success
-   `400 Bad Request`: Validation failure
-   `401 Unauthorized`: Invalid/Missing token
-   `404 Not Found`: Resource not found
-   `500 Internal Server Error`: Server failure

Response Format:
```json
{
  "status": "error",
  "message": "Error description",
  "code": "ERROR_CODE"
}
```
