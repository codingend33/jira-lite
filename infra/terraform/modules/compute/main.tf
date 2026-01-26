# Compute Module - EC2 Instance with Docker

# Get latest Amazon Linux 2 ARM AMI via SSM Parameter Store (official AWS method)
data "aws_ssm_parameter" "amazon_linux_2_arm" {
  name = "/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-arm64-gp2"
}

# SSH Key Pair (create if public key provided, otherwise use existing)
resource "aws_key_pair" "main" {
  count      = var.public_key_path != "" ? 1 : 0
  key_name   = var.key_name
  public_key = file(var.public_key_path)

  tags = {
    Name = "${var.project_name}-${var.environment}-key"
  }
}

# Security Group for EC2
resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-${var.environment}-ec2-sg"
  description = "Security group for EC2 instance"
  vpc_id      = var.vpc_id

  # SSH access (restrict to your IP in production)
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # TODO: Restrict to your IP
  }

  # HTTP for backend API
  ingress {
    description = "Backend API"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-ec2-sg"
  }
}

# User Data script to install Docker and configure environment
locals {
  user_data = <<-EOF
#!/bin/bash
set -e

# Update system
yum update -y

# Install Docker
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip

# Create environment file for backend
cat > /home/ec2-user/.env <<'ENVEOF'
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://${var.rds_endpoint}/${var.rds_db_name}
SPRING_DATASOURCE_USERNAME=${var.rds_username}
SPRING_DATASOURCE_PASSWORD=${var.rds_password}

# Spring profiles
SPRING_PROFILES_ACTIVE=prod

# JWT/Cognito
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=${var.cognito_issuer_uri}

# S3
APP_S3_BUCKET=${var.attachments_bucket_name}
AWS_REGION=ap-southeast-2

# Server
SERVER_PORT=8080
ENVEOF

chown ec2-user:ec2-user /home/ec2-user/.env
chmod 600 /home/ec2-user/.env

# Create deployment script
cat > /home/ec2-user/deploy.sh <<'DEPLOYEOF'
#!/bin/bash
set -e

# Configure AWS region for ECR
export AWS_DEFAULT_REGION=ap-southeast-2

# Login to ECR
aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin ${var.ecr_repository_url}

# Pull latest image
docker pull ${var.ecr_repository_url}:latest

# Stop and remove old container
docker stop jira-backend || true
docker rm jira-backend || true

# Run new container
docker run -d \
  --name jira-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file /home/ec2-user/.env \
  ${var.ecr_repository_url}:latest

# Wait for health check
sleep 10
curl -f http://localhost:8080/api/health || echo "Warning: Health check failed"

echo "Deployment complete!"
DEPLOYEOF

chmod +x /home/ec2-user/deploy.sh
chown ec2-user:ec2-user /home/ec2-user/deploy.sh

# Log completion
echo "User data script completed" > /var/log/user-data-complete.log
EOF
}

# EC2 Instance
resource "aws_instance" "main" {
  ami                    = data.aws_ssm_parameter.amazon_linux_2_arm.value
  instance_type          = var.instance_type
  key_name               = var.key_name
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = var.iam_instance_profile

  user_data = local.user_data

  root_block_device {
    volume_size = 20  # GB (Free Tier: up to 30GB)
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-backend"
  }
}

# Elastic IP (optional but recommended for static IP)
resource "aws_eip" "main" {
  instance = aws_instance.main.id
  domain   = "vpc"

  tags = {
    Name = "${var.project_name}-${var.environment}-eip"
  }
}
