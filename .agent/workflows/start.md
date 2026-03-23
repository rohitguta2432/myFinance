---
description: Start the full myFinance stack locally (backend + frontend). Skips services already running.
---

# Start MyFinance Locally

**Frontend:** Vite dev server on port `3005`
**Backend:** Spring Boot on port `8081` (Maven wrapper)
**DB:** PostgreSQL (must be running — via Docker or system service)

## Steps

// turbo
### 1. Check if Backend is Already Running
```bash
curl -sf http://localhost:8081/api/v1/assessment/profile > /dev/null 2>&1 && echo "BACKEND_RUNNING" || echo "BACKEND_NOT_RUNNING"
```
- If output is `BACKEND_RUNNING` → **skip to Step 3**
- If output is `BACKEND_NOT_RUNNING` → continue to Step 2

// turbo
### 2. Start Backend (Spring Boot)
```bash
cd /home/t0266li/Documents/myFinance/backend && mvn spring-boot:run &
```
Wait ~15 seconds, then verify:
```bash
curl -sf http://localhost:8081/api/v1/assessment/profile > /dev/null 2>&1 && echo "BACKEND_READY" || echo "BACKEND_STILL_STARTING"
```
If still starting, wait another 15 seconds and retry (max 3 attempts).

// turbo
### 3. Check if Frontend is Already Running
```bash
curl -sf http://localhost:3005 > /dev/null 2>&1 && echo "FRONTEND_RUNNING" || echo "FRONTEND_NOT_RUNNING"
```
- If output is `FRONTEND_RUNNING` → **skip to Step 5**
- If output is `FRONTEND_NOT_RUNNING` → continue to Step 4

// turbo
### 4. Start Frontend (Vite)
```bash
cd /home/t0266li/Documents/myFinance && npm run dev &
```
Wait ~5 seconds, then verify:
```bash
curl -sf http://localhost:3005 > /dev/null 2>&1 && echo "FRONTEND_READY" || echo "FRONTEND_STILL_STARTING"
```

### 5. Confirm Both Services
Report the status:
- **Frontend:** http://localhost:3005
- **Backend API:** http://localhost:8081/api/v1/assessment/profile

## Troubleshooting

- **Port conflict:** `lsof -i :8081` or `lsof -i :3005` to find conflicting processes
- **DB not running:** `sudo systemctl start postgresql` or `docker compose up -d postgres`
- **Backend build failure:** `cd backend && mvn clean compile` to check for errors
- **Kill everything:** `lsof -ti :8081 | xargs kill -9; lsof -ti :3005 | xargs kill -9`
