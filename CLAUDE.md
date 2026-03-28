# MyFinance

Personal finance assessment and advisory platform for Indian users. 6-step wizard (profile, cash flow, net worth, goals, insurance, tax) with AI-powered insights via AWS Bedrock.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + Vite 7 + Tailwind CSS 4 |
| State | Zustand (auth) + TanStack React Query (server state) |
| Backend | Spring Boot 3.4.1, Java 21, Maven |
| Database | PostgreSQL 14 |
| ORM | Hibernate/JPA (ddl-auto: update) |
| Auth | Google OAuth 2.0 (ID token verification) |
| AI | AWS Bedrock (Amazon Nova Lite) |
| Containers | Docker + Docker Compose |
| Web Server | Nginx (reverse proxy in production) |

## Project Structure

```
myFinance/
├── backend/                  # Spring Boot backend
│   ├── src/main/java/com/myfinance/
│   │   ├── controller/       # REST controllers (/api/v1/*)
│   │   ├── service/          # Business logic
│   │   ├── model/            # JPA entities
│   │   ├── dto/              # Data transfer objects
│   │   ├── repository/       # Spring Data JPA repositories
│   │   └── config/           # CORS, security, Bedrock config
│   └── src/main/resources/
│       └── application.yml   # App config
├── src/                      # React frontend
│   ├── features/             # Feature modules (auth, assessment, dashboard)
│   ├── components/           # Shared UI components
│   ├── hooks/                # Custom React hooks
│   ├── services/             # API client (native fetch)
│   ├── constants/            # Routes, enums
│   └── utils/                # Helpers
├── specs/                    # Architecture & business specs
├── docker-compose.yml        # Full-stack orchestration
├── Dockerfile                # Frontend (multi-stage, nginx)
├── nginx.conf                # Reverse proxy config
└── deploy_aws.sh             # EC2 deployment script
```

## Running Locally

**Prerequisites:** PostgreSQL running on localhost:5432 (or `docker compose up postgres`)

```bash
# Backend (port 8081)
cd backend && mvn spring-boot:run

# Frontend (port 3005, proxies /api to :8081)
npm run dev
```

Or use the `/start` skill which handles both.

## Build & Deploy

```bash
# Frontend build
npm run build

# Backend build
cd backend && mvn clean package -DskipTests

# Full stack via Docker
docker compose up -d --build

# AWS EC2 deploy
./deploy_aws.sh
```

## Key Conventions

### Backend
- All REST endpoints under `/api/v1/`
- Controllers use `@RestController` with `@RequestMapping`
- User identification via `X-User-Id` header (set by frontend from auth store)
- Entities use `@Builder` (Lombok) pattern
- Repositories extend `JpaRepository` with `findByUserId` methods
- No explicit Flyway migrations; Hibernate auto-generates schema

### Frontend
- Feature-based folder structure (`features/auth`, `features/assessment`, `features/dashboard`)
- API calls via native `fetch` in `src/services/api.js` — no Axios
- Auth token stored in localStorage via Zustand persist (`auth-storage`)
- Protected routes via `ProtectedRoute` HOC
- Tailwind CSS for styling — no CSS modules or styled-components
- `clsx` + `tailwind-merge` for conditional class merging

### Environment Variables

**Frontend (.env):**
- `VITE_GOOGLE_CLIENT_ID` — Google OAuth client ID
- `VITE_AUTH_REQUIRED` — toggle auth enforcement (true/false)

**Backend (application.yml / env):**
- `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` — DB connection
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` — Bedrock access
- `GOOGLE_CLIENT_ID` — OAuth verification

## Ports

| Service | Local Dev | Docker |
|---------|-----------|--------|
| Frontend | 3005 | 80 |
| Backend | 8081 | 8080 → 8081 |
| PostgreSQL | 5432 | 5432 |

## Lint

```bash
npm run lint    # ESLint for frontend
```

## Specs & Docs

Business and architecture specs live in `specs/` — read these before making changes to assessment flow or API contracts.
