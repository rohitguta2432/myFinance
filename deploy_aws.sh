#!/bin/bash
set -e

echo "Starting deployment of MyFinance to AWS EC2..."

# 1. Variables
AMI_ID="ami-04680790a315cd58d" # Ubuntu 22.04 LTS us-east-1
INSTANCE_TYPE="t3.micro"
KEY_NAME="myfinance-key"
SG_NAME="myfinance-sg-1"

# 2. Key Pair Check
if [ ! -f "${KEY_NAME}.pem" ]; then
    echo "Key pair ${KEY_NAME}.pem not found. It should have been created."
    exit 1
fi

# 3. Create Security Group Settings
echo "Creating/Verifying Security Group (${SG_NAME})..."
SG_ID=$(aws ec2 describe-security-groups --group-names ${SG_NAME} --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)

if [ "$SG_ID" == "None" ] || [ -z "$SG_ID" ]; then
    VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query "Vpcs[0].VpcId" --output text)
    SG_ID=$(aws ec2 create-security-group --group-name ${SG_NAME} --description "Security group for MyFinance" --vpc-id ${VPC_ID} --query "GroupId" --output text)
    echo "Created SG: $SG_ID"

    aws ec2 authorize-security-group-ingress --group-id ${SG_ID} --protocol tcp --port 22 --cidr 0.0.0.0/0 >/dev/null
    aws ec2 authorize-security-group-ingress --group-id ${SG_ID} --protocol tcp --port 80 --cidr 0.0.0.0/0 >/dev/null
    aws ec2 authorize-security-group-ingress --group-id ${SG_ID} --protocol tcp --port 8080 --cidr 0.0.0.0/0 >/dev/null
else
    echo "SG already exists: $SG_ID"
fi

# 4. Launch EC2 Instance with User Data to install Docker
echo "Launching EC2 Instance..."

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
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=MyFinance-App}]" \
    --query "Instances[0].InstanceId" \
    --output text)

echo "Instance launched: $INSTANCE_ID"

# 5. Wait for the Instance to be running and get Public IP
echo "Waiting for instance to be in 'running' state..."
aws ec2 wait instance-running --instance-ids ${INSTANCE_ID}
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids ${INSTANCE_ID} --query "Reservations[0].Instances[0].PublicIpAddress" --output text)
echo "Instance is running. Public IP: ${PUBLIC_IP}"

# 6. Wait for SSH to become available
echo "Waiting for SSH to become available on ${PUBLIC_IP}..."
until ssh -o StrictHostKeyChecking=no -o ConnectTimeout=2 -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP} "docker --version &>/dev/null && echo 'Ready!'"; do
    printf "."
    sleep 5
done
echo "SSH and Docker are ready!"

# 7. Sync the code to the instance (excluding node_modules and target files)
echo "Syncing application code to the EC2 instance..."
rsync -avz -e "ssh -o StrictHostKeyChecking=no -i ${KEY_NAME}.pem" \
    --exclude 'node_modules' \
    --exclude 'backend/target' \
    --exclude '.git' \
    --exclude '*.pem' \
    ./ ubuntu@${PUBLIC_IP}:/home/ubuntu/app/

# 8. Run Docker Compose on the Instance
echo "Building and bringing up Docker containers remotely..."
ssh -o StrictHostKeyChecking=no -i ${KEY_NAME}.pem ubuntu@${PUBLIC_IP} << 'EOF'
cd /home/ubuntu/app
sudo docker compose up -d --build
EOF

echo "Deployment complete!🚀"
echo "You can access the UI at: http://${PUBLIC_IP}"
echo "You can access the API at: http://${PUBLIC_IP}:8080/api/v1/assessment/profile"
