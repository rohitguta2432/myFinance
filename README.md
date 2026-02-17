# MyFinance PWA & Backend

A comprehensive personal finance management application featuring a Progressive Web App (PWA) frontend and a robust Spring Boot backend.

## 🚀 Features

*   **Financial Assessment:** Step-by-step wizard to capture your financial profile.
*   **Net Worth Tracking:** Track Assets vs. Liabilities.
*   **Income & Expense Management:** Monthly cash flow analysis.
*   **Goal Setting:** Plan for future financial goals.
*   **Insurance Gap Analysis:** Identify coverage needs.
*   **Tax Optimization:** Compare Old vs. New tax regimes (India-specific logic).
*   **Dashboard:** Visual representation of your financial health.

## 🛠 Tech Stack

### Frontend (PWA)
*   **Framework:** React 18+
*   **Build Tool:** Vite
*   **Styling:** Tailwind CSS v4
*   **State Management:** Zustand
*   **PWA:** `vite-plugin-pwa` for offline capabilities and installability.

### Backend (API)
*   **Framework:** Spring Boot 3.4.2 (Java 21)
*   **Database:** PostgreSQL 14+
*   **ORM:** Spring Data JPA (Hibernate)
*   **Migration:** Flyway
*   **Build Tool:** Maven

## 📂 Project Structure

```
myFinance/
├── spec/                 # Architectural specifications & designs
├── server/               # Backend code
│   └── myfinance-backend/
├── src/                  # Frontend source code
├── public/               # Static assets
└── index.html            # App entry point
```

## ⚡ Getting Started

### Prerequisites

*   Node.js 18+ & npm
*   Java JDK 21
*   Maven 3.8+
*   PostgreSQL 14+ running locally

### 1. Database Setup

Ensure PostgreSQL is running and create the database:

```bash
# If using the default config
createdb -U postgres myfinance
```

Update `server/myfinance-backend/src/main/resources/application.properties` with your database credentials if they differ from the defaults (`postgres`/`postgres`).

### 2. Backend Setup

Navigate to the backend directory and run the application:

```bash
cd server/myfinance-backend
mvn spring-boot:run
```

The API will start at `http://localhost:8080`.
Swagger UI (if configured) would typically be at `http://localhost:8080/swagger-ui.html`.

### 3. Frontend Setup

In the root directory:

```bash
# Install dependencies
npm install

# Run development server
npm run dev
```

The application will be available at `http://localhost:5173`.

## 📖 Documentation

Detailed specifications can be found in the `specs/` directory:
*   [Backend Architecture](specs/backend_architecture.md)
*   [Database Design](specs/database_design.md)
*   [API Specification](specs/api_specification.md)
*   [Frontend Specification](specs/frontend_specification.md)

## 🤝 Contributing

1.  Fork the repository
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request
