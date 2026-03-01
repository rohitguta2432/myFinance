---
description: Deploy latest changes to AWS EC2 (98.81.216.108)
---

# Deploy MyFinance to AWS EC2

**Target:** `ubuntu@98.81.216.108`
**Key:** `myfinance-key.pem` (project root)
**Stack:** Docker Compose → Postgres 14 + Spring Boot 3.4 backend (port 8081→8080) + Vite/Nginx frontend (port 80)

## Prerequisites
- `myfinance-key.pem` exists in project root with `chmod 400`
- AWS EC2 instance is running (check: `ssh -i myfinance-key.pem ubuntu@98.81.216.108 "docker ps"`)

## Steps

### 1. Commit & Push Code
```bash
cd /home/t0266li/Documents/myFinance
git add -A
git commit -m "your commit message"
git push origin main
```

// turbo
### 2. Sync Code to EC2
```bash
cd /home/t0266li/Documents/myFinance
rsync -avz -e "ssh -o StrictHostKeyChecking=no -i myfinance-key.pem" \
    --exclude 'node_modules' \
    --exclude 'backend/target' \
    --exclude '.git' \
    --exclude '*.pem' \
    --exclude 'cloudflare*' \
    ./ ubuntu@98.81.216.108:/home/ubuntu/app/
```

// turbo
### 3. Rebuild & Restart Docker Containers
```bash
ssh -o StrictHostKeyChecking=no -i /home/t0266li/Documents/myFinance/myfinance-key.pem ubuntu@98.81.216.108 "cd /home/ubuntu/app && sudo docker compose up -d --build"
```

// turbo
### 4. Verify Deployment
```bash
ssh -o StrictHostKeyChecking=no -i /home/t0266li/Documents/myFinance/myfinance-key.pem ubuntu@98.81.216.108 "sudo docker ps"
```

The app should be accessible at:
- **Frontend:** http://98.81.216.108
- **API:** http://98.81.216.108:8080/api/v1/assessment/profile

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  EC2 Instance: 98.81.216.108 (t3.micro, Ubuntu 22.04)  │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Postgres   │  │   Backend    │  │   Frontend   │  │
│  │   :5432      │←─│  :8081       │←─│  Nginx :80   │  │
│  │  (pgdata)    │  │  Spring Boot │  │  (SPA + API  │  │
│  │              │  │  Java 21     │  │   proxy)     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Troubleshooting

- **Check container logs:** `ssh -i myfinance-key.pem ubuntu@98.81.216.108 "sudo docker compose -f /home/ubuntu/app/docker-compose.yml logs --tail 50 frontend"`
- **Restart a single service:** `ssh -i myfinance-key.pem ubuntu@98.81.216.108 "cd /home/ubuntu/app && sudo docker compose restart frontend"`
- **Full rebuild (no cache):** `ssh -i myfinance-key.pem ubuntu@98.81.216.108 "cd /home/ubuntu/app && sudo docker compose build --no-cache && sudo docker compose up -d"`
