# IMPL: Backend Architecture & Database Design

## Goal
Design the backend architecture for the MyFinance application using Java 21, Spring Boot 3.x (latest), and PostgreSQL. The design will be driven by the data requirements observed in the Stitch frontend screens (Steps 1-9).

## Technology Stack
-   **Language:** Java 21
-   **Framework:** Spring Boot 3.4+
-   **Database:** PostgreSQL 16+
-   **ORM:** Spring Data JPA (Hibernate)
-   **Migration:** Flyway or Liquibase
-   **API:** RESTful

## Data Analysis (Draft)

Based on the 6-step assessment flow:

1.  **User/Profile (Step 1):** Age, Marital Status, Dependents, Risk Tolerance (Conservative/Moderate/Aggressive).
2.  **Income & Expenses (Step 2):** Monthly Income (Salary, Business, Other), Monthly Expenses (Rent/EMI, Food, Transport, Lifestyle).
3.  **Assets & Liabilities (Step 3):**
    -   **Assets:** Bank Balance, Investments (Stocks/MFs), Gold, Real Estate, EPF/PPF.
    -   **Liabilities:** Home Loan, Car Loan, Personal Loan, Credit Card Debt.
4.  **Goals (Step 4):** Goal Name (e.g., Retirement, House, Car), Target Amount, Time Horizon (Years).
5.  **Insurance (Step 5):** Life Insurance Cover, Health Insurance Cover.
6.  **Tax (Step 6):** Existing 80C Investments, Health Premium (80D), NPS (80CCD).

## Proposed Schema

### Tables

-   `users`: Core user identity.
-   `financial_profiles`: 1:1 with users. Stores Step 1 & 2 data (Age, Income, Expenses).
-   `assets`: 1:N with users. Type (enum), Value, Description.
-   `liabilities`: 1:N with users. Type (enum), Outstanding Amount, EMI.
-   `goals`: 1:N with users. Name, Target Amount, Target Date/Years.
-   `insurances`: 1:N with users. Type (Life/Health), Coverage Amount, Premium.
-   `tax_details`: 1:1 with users. 80C, 80D, etc.

## API Structure

-   `POST /api/v1/assessment/step1` (Profile)
-   `POST /api/v1/assessment/step2` (Income/Expense)
-   ...
-   `GET /api/v1/dashboard/summary` (Calculated Net Worth, Health Score)

## Next Steps
1.  Refine field names based on actual HTML form inputs.
2.  Create a detailed SQL/ERD artifact.
3.  Generate Spring Boot project structure.
