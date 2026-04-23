#!/bin/bash
# Unified deploy script. Idempotent.
#
#   First run  (no instance):   provisions EC2, SG, docker, runs init-letsencrypt
#                               + full docker compose stack (postgres, nginx, backend).
#   Subsequent runs:            finds existing instance by Name tag and does a fast
#                               backend-only deploy (rsync src + compose up --build backend).
#
# Pre-reqs:
#   - myfinance-key.pem in this directory
#   - backend/target/myfinance-backend-0.0.1-SNAPSHOT.jar built (mvn clean package -DskipTests)
#   - aws cli configured
set -e

echo "▶ MyFinance deploy — AWS EC2"

# ── Config ─────────────────────────────────────────────────────────────
AMI_ID="ami-04680790a315cd58d"        # Ubuntu 22.04 LTS us-east-1
INSTANCE_TYPE="t3.small"
KEY_NAME="myfinance-key"
SG_NAME="myfinance-sg-1"
INSTANCE_NAME="MyFinance-App"
BACKEND_HOST="api-preprod.myfinancial.in"
JAR_FILE="backend/target/myfinance-backend-0.0.1-SNAPSHOT.jar"
REMOTE_DIR="/home/ubuntu/app"

# ── Pre-flight ─────────────────────────────────────────────────────────
if [ ! -f "${KEY_NAME}.pem" ]; then
    echo "✗ Key pair ${KEY_NAME}.pem not found."
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "✗ Backend JAR not found. Run: cd backend && mvn clean package -DskipTests"
    exit 1
fi

# ── Security Group ─────────────────────────────────────────────────────
echo "▶ Checking Security Group ${SG_NAME}..."
SG_ID=$(aws ec2 describe-security-groups --group-names ${SG_NAME} --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)

if [ "$SG_ID" == "None" ] || [ -z "$SG_ID" ]; then
    VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query "Vpcs[0].VpcId" --output text)
    SG_ID=$(aws ec2 create-security-group --group-name ${SG_NAME} --description "Security group for MyFinance" --vpc-id ${VPC_ID} --query "GroupId" --output text)
    echo "  created SG: $SG_ID"
    for port in 22 80 443 8080; do
        aws ec2 authorize-security-group-ingress --group-id ${SG_ID} --protocol tcp --port $port --cidr 0.0.0.0/0 >/dev/null
    done
else
    echo "  SG exists: $SG_ID"
fi

# ── Instance discovery — reuse or provision ────────────────────────────
echo "▶ Looking up existing instance tagged Name=${INSTANCE_NAME}..."
EXISTING=$(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=${INSTANCE_NAME}" "Name=instance-state-name,Values=pending,running" \
    --query "Reservations[].Instances[].[InstanceId,PublicIpAddress]" \
    --output text)
RUNNING_COUNT=$(printf "%s\n" "$EXISTING" | grep -c . || true)

if [ "$RUNNING_COUNT" -gt 1 ]; then
    echo "✗ Found ${RUNNING_COUNT} running instances tagged ${INSTANCE_NAME}:"
    printf "%s\n" "$EXISTING"
    echo "  Terminate the zombies before re-deploying."
    exit 1
fi

MODE=""
if [ "$RUNNING_COUNT" -eq 1 ]; then
    INSTANCE_ID=$(printf "%s\n" "$EXISTING" | awk '{print $1}')
    PUBLIC_IP=$(printf "%s\n" "$EXISTING" | awk '{print $2}')
    MODE="fast"
    echo "  reusing ${INSTANCE_ID} (${PUBLIC_IP}) — fast deploy mode"
else
    MODE="bootstrap"
    echo "  no instance found — bootstrap mode (provision + full stack)"

    USER_DATA=$(cat << 'EOF'
#!/bin/bash
apt-get update -y
apt-get install -y ca-certificates curl gnupg
mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
usermod -aG docker ubuntu
systemctl enable docker
systemctl start docker
mkdir -p /home/ubuntu/app
chown ubuntu:ubuntu /home/ubuntu/app
EOF
)

    INSTANCE_ID=$(aws ec2 run-instances \
        --image-id ${AMI_ID} \
        --count 1 \
        --instance-type ${INSTANCE_TYPE} \
        --key-name ${KEY_NAME} \
        --security-group-ids ${SG_ID} \
        --user-data "$USER_DATA" \
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${INSTANCE_NAME}}]" \
        --query "Instances[0].InstanceId" \
        --output text)

    echo "  launched $INSTANCE_ID"
    aws ec2 wait instance-running --instance-ids ${INSTANCE_ID}
    PUBLIC_IP=$(aws ec2 describe-instances --instance-ids ${INSTANCE_ID} --query "Reservations[0].Instances[0].PublicIpAddress" --output text)
    echo "  running at ${PUBLIC_IP}"

    echo "▶ Waiting for SSH + docker on fresh instance..."
    until ssh -o StrictHostKeyChecking=no -o ConnectTimeout=2 -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP} "docker --version &>/dev/null && echo ready"; do
        printf "."
        sleep 5
    done
    echo "  ready"
fi

SSH_OPTS="-o StrictHostKeyChecking=no -i ${KEY_NAME}.pem"

# ── Sync backend src (needed for docker build context) ─────────────────
echo "▶ Syncing backend src to ${PUBLIC_IP}:${REMOTE_DIR}/backend/..."
ssh ${SSH_OPTS} ubuntu@${PUBLIC_IP} "mkdir -p ${REMOTE_DIR}/backend"
rsync -az --delete -e "ssh ${SSH_OPTS}" \
    backend/src backend/pom.xml backend/Dockerfile \
    ubuntu@${PUBLIC_IP}:${REMOTE_DIR}/backend/

# ── Mode-specific actions ──────────────────────────────────────────────
if [ "$MODE" = "bootstrap" ]; then
    echo "▶ First-time bootstrap: uploading full stack config..."
    rsync -az -e "ssh ${SSH_OPTS}" \
        docker-compose.yml nginx.conf init-letsencrypt.sh \
        ubuntu@${PUBLIC_IP}:${REMOTE_DIR}/

    ssh ${SSH_OPTS} ubuntu@${PUBLIC_IP} bash <<'REMOTE'
set -e
cd /home/ubuntu/app
chmod +x init-letsencrypt.sh
sudo ./init-letsencrypt.sh
REMOTE
else
    echo "▶ Fast deploy: rebuilding backend container..."
    ssh ${SSH_OPTS} ubuntu@${PUBLIC_IP} bash <<'REMOTE'
set -e
cd /home/ubuntu/app
sudo docker compose up -d --force-recreate --build backend
echo "▶ Waiting for backend health..."
for i in {1..30}; do
    if sudo docker exec myfinance-backend curl -sf http://localhost:8081/swagger-ui.html >/dev/null 2>&1 \
       || sudo docker exec myfinance-backend wget -qO- http://localhost:8081/swagger-ui.html >/dev/null 2>&1; then
        echo "✓ backend up"; break
    fi
    sleep 2
done
REMOTE
fi

# ── Verify ─────────────────────────────────────────────────────────────
echo "▶ Verifying ${BACKEND_HOST}..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 https://${BACKEND_HOST}/swagger-ui.html)
echo "  swagger-ui: ${HTTP_STATUS}"

if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "302" ]; then
    echo ""
    echo "✅ Deploy complete"
    echo "   Backend API: https://${BACKEND_HOST}/api/v1/"
    echo "   Swagger UI:  https://${BACKEND_HOST}/swagger-ui.html"
    echo "   Frontend:    https://myfinancial.in (AWS Amplify, separate pipeline)"
else
    echo ""
    echo "⚠ Verification returned HTTP ${HTTP_STATUS} — investigate on EC2:"
    echo "   ssh ${SSH_OPTS} ubuntu@${PUBLIC_IP} 'sudo docker logs myfinance-backend --tail 50'"
    exit 1
fi
