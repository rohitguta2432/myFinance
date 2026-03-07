# IMPL: Backend Architecture & Database Design

## Goal
Design the backend architecture for the MyFinance application using Java 21, Spring Boot 3.x, and PostgreSQL. The design is explicitly aligned with the 6-step assessment flow and is actively implemented in the current MVP.

## Technology Stack
-   **Language:** Java 21
-   **Framework:** Spring Boot 3.4.2
-   **Database:** PostgreSQL 16+
-   **ORM:** Spring Data JPA (Hibernate)
-   **API:** RESTful (JSON)

## Data Analysis & Implemented Schema

Based on the 6-step assessment flow, the backend implements the following `Entity` models mapped directly to PostgreSQL tables.

*(Note: In the current single-session MVP, `user_id` mapping is bypassed. Entities use `BIGINT` auto-generated Long IDs.)*

1.  **User/Profile (Step 1):** Map to `profiles` table. Includes Age, City, Marital Status, Dependents (Child/Adult), Employment Type, Residency Status, Risk Tolerance, Risk Score, and a serialized JSON string of `riskAnswers`.
2.  **Income & Expenses (Step 2):** 
    -   `incomes` table: Source Name, Amount, Frequency, Tax Deducted, TDS Percentage.
    -   `expenses` table: Category, Amount, Frequency, Is Essential flag.
3.  **Assets & Liabilities (Step 3):**
    -   `assets` table: Asset Type (Equity, Real Estate, etc.), Name, Current Value, Allocation Percentage.
    -   `liabilities` table: Liability Type (Home Loan, etc.), Name, Outstanding Amount, Monthly EMI, Interest Rate.
4.  **Goals (Step 4):** `goals` table. Goal Type, Name, Target Amount, Current Cost, Time Horizon (Years), Inflation Rate.
5.  **Insurance (Step 5):** `insurance_policies` table. Insurance Type (Life/Health), Policy Name, Coverage Amount, Premium Amount, Renewal Date.
6.  **Tax (Step 6):** `tax_plans` table. Selected Regime, PPF/ELSS, EPF/VPF, Tuition Fees, LIC, Home Loan Principal, Health Premium, Parent's Health Premium, Calculated Tax (Old), Calculated Tax (New).

## API Structure

The `AssessmentController` (`/api/v1/assessment`) exposes these REST endpoints:

-   `GET/POST /profile` (Step 1)
-   `GET /financials`, `POST /income`, `POST /expense` (Step 2)
-   `GET /balance-sheet`, `POST /asset`, `POST /liability` (Step 3)
-   `GET/POST /goals` (Step 4)
-   `GET/POST /insurance` (Step 5)
-   `GET/POST /tax` (Step 6)

*Refer to `api_specification.md` and `database_design.md` for exact DTO payloads and SQL schemas.*
