# API Specification: MyFinance

## Overview
This document outlines the REST API endpoints for the MVP MyFinance application. It perfectly maps to the current Spring Boot backend implementation (`AssessmentController` and associated `DTO`s).

## Base URL
`/api/v1`

*Note: In the current single-session MVP, authentication headers are bypassed or mocked. A future authentication iteration will require Bearer Tokens.*

## Endpoints

### 1. Assessment Flow (Wizard)

**Step 1: Profile & Risk**
-   `GET /assessment/profile`
    -   *Returns:* `ProfileDTO`
-   `POST /assessment/profile`
    -   *Description:* Save/Update profile and risk inputs.
    -   *Payload/Returns `ProfileDTO`:* 
        ```json
        {
          "id": 1,
          "age": 30,
          "city": "Mumbai",
          "maritalStatus": "MARRIED",
          "dependents": 2,
          "childDependents": 1,
          "employmentType": "SALARIED",
          "residencyStatus": "RESIDENT",
          "riskTolerance": "MODERATE",
          "riskScore": 7,
          "riskAnswers": { "1": 3, "2": 2 }
        }
        ```

**Step 2: Income & Expenses**
-   `GET /assessment/financials`
    -   *Returns:* `FinancialsResponse` (contains lists of incomes and expenses)
-   `POST /assessment/income`
    -   *Payload/Returns `IncomeDTO`:*
        ```json
        {
          "id": 1,
          "sourceName": "Salary",
          "amount": 150000.0,
          "frequency": "MONTHLY",
          "taxDeducted": true,
          "tdsPercentage": 10.0
        }
        ```
-   `POST /assessment/expense`
    -   *Payload/Returns `ExpenseDTO`:*
        ```json
        {
          "id": 1,
          "category": "Rent",
          "amount": 40000.0,
          "frequency": "MONTHLY",
          "isEssential": true
        }
        ```

**Step 3: Assets & Liabilities**
-   `GET /assessment/balance-sheet`
    -   *Returns:* `BalanceSheetResponse` (contains lists of assets and liabilities)
-   `POST /assessment/asset`
    -   *Payload/Returns `AssetDTO`:*
        ```json
        {
          "id": 1,
          "assetType": "EQUITY",
          "name": "Mutual Funds",
          "currentValue": 500000.0,
          "allocationPercentage": 60.0
        }
        ```
-   `POST /assessment/liability`
    -   *Payload/Returns `LiabilityDTO`:*
        ```json
        {
          "id": 1,
          "liabilityType": "HOME_LOAN",
          "name": "HDFC Home Loan",
          "outstandingAmount": 4500000.0,
          "monthlyEmi": 45000.0,
          "interestRate": 8.5
        }
        ```

**Step 4: Financial Goals**
-   `GET /assessment/goals`
    -   *Returns:* `List<GoalDTO>`
-   `POST /assessment/goal`
    -   *Payload/Returns `GoalDTO`:*
        ```json
        {
          "id": 1,
          "goalType": "HOME",
          "name": "Buy 3BHK",
          "targetAmount": 15000000.0,
          "currentCost": 10000000.0,
          "timeHorizonYears": 5,
          "inflationRate": 6.0
        }
        ```

**Step 5: Insurance**
-   `GET /assessment/insurance`
    -   *Returns:* `List<InsuranceDTO>`
-   `POST /assessment/insurance`
    -   *Payload/Returns `InsuranceDTO`:*
        ```json
        {
          "id": 1,
          "insuranceType": "HEALTH",
          "policyName": "Family Floater",
          "coverageAmount": 1000000.0,
          "premiumAmount": 15000.0,
          "renewalDate": "2025-01-15"
        }
        ```

**Step 6: Tax Planning**
-   `GET /assessment/tax`
    -   *Returns:* `TaxDTO`
-   `POST /assessment/tax`
    -   *Payload/Returns `TaxDTO`:*
        ```json
        {
          "id": 1,
          "selectedRegime": "NEW",
          "ppfElssAmount": 150000.0,
          "epfVpfAmount": 0.0,
          "tuitionFeesAmount": 0.0,
          "licPremiumAmount": 0.0,
          "homeLoanPrincipal": 0.0,
          "healthInsurancePremium": 25000.0,
          "parentsHealthInsurance": 50000.0,
          "calculatedTaxOld": 250000.0,
          "calculatedTaxNew": 180000.0
        }
        ```

## Error Handling
Standard HTTP Status Codes:
-   `200 OK`: Success
-   `400 Bad Request`: Validation failure
-   `404 Not Found`: Resource not found
-   `500 Internal Server Error`: Server failure
