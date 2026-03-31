#!/bin/bash
set -e

# Fast deploy — ships pre-built artifacts to EC2, no Docker build on server
# Requires: pre-built dist/ (npm run build) and backend JAR (mvn package)

EC2_HOST="app.myfinancial.in"
KEY="myfinance-key.pem"
SSH_OPTS="-o StrictHostKeyChecking=no -i ${KEY}"
REMOTE_USER="ubuntu"
REMOTE_DIR="/home/ubuntu/app"

JAR_FILE="backend/target/myfinance-backend-0.0.1-SNAPSHOT.jar"
DIST_DIR="dist"

# ── Pre-flight ──
echo "▶ Checking pre-built artifacts..."

if [ ! -f "$JAR_FILE" ]; then
    echo "✗ Backend JAR not found. Run: cd backend && mvn clean package -DskipTests"
    exit 1
fi

if [ ! -d "$DIST_DIR" ]; then
    echo "✗ Frontend dist/ not found. Run: npm run build"
    exit 1
fi

echo "✓ JAR: $JAR_FILE"
echo "✓ Frontend: $DIST_DIR/"

# ── Upload artifacts ──
echo ""
echo "▶ Uploading artifacts to ${EC2_HOST}..."

# Upload JAR
echo "  → Backend JAR..."
scp ${SSH_OPTS} "$JAR_FILE" ${REMOTE_USER}@${EC2_HOST}:${REMOTE_DIR}/app.jar

# Upload frontend dist
echo "  → Frontend dist/..."
rsync -az --delete -e "ssh ${SSH_OPTS}" \
    ${DIST_DIR}/ ${REMOTE_USER}@${EC2_HOST}:${REMOTE_DIR}/dist/

# Upload nginx config (in case it changed)
echo "  → nginx.conf..."
scp ${SSH_OPTS} nginx.conf ${REMOTE_USER}@${EC2_HOST}:${REMOTE_DIR}/nginx.conf

# Upload docker-compose for DB + nginx + certbot
echo "  → docker-compose.yml..."
scp ${SSH_OPTS} docker-compose.fast.yml ${REMOTE_USER}@${EC2_HOST}:${REMOTE_DIR}/docker-compose.yml

echo "✓ Upload complete"

# ── Restart services ──
echo ""
echo "▶ Restarting services on ${EC2_HOST}..."

ssh ${SSH_OPTS} ${REMOTE_USER}@${EC2_HOST} << 'REMOTE'
set -e
cd /home/ubuntu/app

# Restart Docker services (postgres, nginx, certbot)
docker compose up -d --force-recreate frontend

# Restart backend JAR (systemd or direct)
if systemctl is-active --quiet myfinance-backend 2>/dev/null; then
    sudo systemctl restart myfinance-backend
else
    # Kill old backend if running
    pkill -f 'app.jar' 2>/dev/null || true
    sleep 1

    # Start backend with env vars from .env
    if [ -f .env ]; then
        set -a; source .env; set +a
    fi

    nohup java -jar app.jar \
        --server.port=8081 \
        > /home/ubuntu/app/backend.log 2>&1 &

    echo "Backend PID: $!"
fi

# Wait for backend to be healthy
echo "Waiting for backend..."
for i in {1..20}; do
    if curl -sf http://localhost:8081/api/v1/assessment/profile > /dev/null 2>&1; then
        echo "✓ Backend is healthy"
        break
    fi
    sleep 2
done
REMOTE

echo ""
echo "▶ Verifying deployment..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 https://${EC2_HOST}/)
echo "  HTTPS status: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" = "200" ]; then
    echo ""
    echo "✅ Deploy complete! https://${EC2_HOST}"
else
    echo ""
    echo "⚠ Site returned HTTP ${HTTP_STATUS} — check logs on EC2"
fi
