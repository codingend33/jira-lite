# AWS Deployment Getting Started Guide

This guide will walk you through setting up your AWS deployment from scratch.

## Prerequisites

Before interacting with the cloud, ensure you have:
- ✅ AWS Account (Free Tier preferred)
- ✅ Terraform >= 1.6.0 installed locally
- ✅ AWS CLI v2 installed locally
- ✅ GitHub Account with repository access
- ✅ Local git repository pushed to GitHub

## Step 1: Configure AWS CLI

### 1.1 Install AWS CLI

**Windows**:
Download and install from: https://awscli.amazonaws.com/AWSCLIV2.msi

**Verify**:
```bash
aws --version
```

### 1.2 Create IAM User (for local deployment)

1. Log in to AWS Console -> **IAM** -> **Users** -> **Create user**
2. Username: `terraform-deploy`
3. Click **Next**
4. Select **Attach policies directly**
5. Add `AdministratorAccess` (recommended for first-time setup)
6. Click **Create user**

### 1.3 Create Access Keys

1. Click on the user `terraform-deploy`
2. Go to **Security credentials** tab
3. **Access keys** -> **Create access key**
4. Use case: **Command Line Interface (CLI)**
5. **Important**: Copy Access Key ID and Secret Access Key immediately.

### 1.4 Configure Local CLI

```bash
aws configure
```

Input:
- AWS Access Key ID: <Your Key ID>
- AWS Secret Access Key: <Your Secret Key>
- Default region name: `ap-southeast-2`
- Default output format: `json`

Verify:
```bash
aws sts get-caller-identity
```

## Step 2: Prepare Terraform Configuration

### 2.1 Get Account ID

```bash
aws sts get-caller-identity --query Account --output text
```

### 2.2 Create EC2 SSH Key Pair

1. AWS Console -> **EC2** -> **Key Pairs** -> **Create key pair**
2. Name: `jira-lite-prod-key`
3. Type: **RSA**, Format: **.pem**
4. Save the file securely (e.g., `~/.ssh/jira-lite-prod-key.pem`)

### 2.3 Configure Variables

```bash
cd infra/terraform
copy terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
aws_region   = "ap-southeast-2"
project_name = "jira-lite"
environment  = "prod"

# EC2
ec2_instance_type = "t4g.micro"
ec2_key_name      = "jira-lite-prod-key"

# RDS (Strong password required!)
rds_password          = "YourSecurePassword123!@#"

# S3 Buckets (Must be globally unique)
attachments_bucket_name = "jira-lite-attachments-<YOUR_ACCOUNT_ID>"
frontend_bucket_name    = "jira-lite-frontend-<YOUR_ACCOUNT_ID>"

# GitHub
github_org  = "your-username"
github_repo = "jira-lite"
```

## Step 3: Initialize State Backend

```bash
cd infra/scripts
# Windows (Git Bash)
./bootstrap-state.sh
```

**Update `backend.tf`**:
Edit `infra/terraform/backend.tf` and replace `<YOUR_ACCOUNT_ID>` with your actual AWS Account ID.

## Step 4: Deploy Infrastructure

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

Type `yes` to confirm. This takes ~10-15 minutes.

**Save Outputs**:
```bash
terraform output > outputs.txt
```

## Step 5: Configure Cognito Trigger (Manual)

1. AWS Console -> **Cognito** -> **User Pools** -> Select Pool
2. **Triggers** tab -> **Pre token generation**
3. Select Lambda: `jira-lite-prod-pre-token-generation`
4. Save changes.

## Step 6: Configure GitHub Actions

### 6.1 Secrets (Settings -> Secrets and variables -> Actions)

| Name | Value | Source |
|------|-------|--------|
| `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::...` | `terraform output github_actions_role_arn` |
| `EC2_SSH_KEY` | `-----BEGIN RSA...` | Content of your `.pem` file |

### 6.2 Variables

| Name | Value Example | Source |
|------|---------------|--------|
| `AWS_REGION` | `ap-southeast-2` | - |
| `ECR_REPOSITORY` | `...dkr.ecr...` | `terraform output ecr_repository_url` |
| `EC2_HOST` | `54.x.x.x` | `terraform output ec2_public_ip` |
| `FRONTEND_BUCKET` | `jira-lite-frontend-...` | Your bucket name |
| `CF_DIST_ID` | `E123...` | `terraform output cloudfront_distribution_id` |
| `VITE_API_BASE_URL` | `http://<IP>:8080` | `terraform output ec2_public_ip` |
| `VITE_COGNITO_DOMAIN` | `https://...` | `terraform output cognito_auth_url` |
| `VITE_COGNITO_CLIENT_ID`| `1a2b...` | Your Client ID |
| `VITE_COGNITO_REDIRECT_URI` | `https://<CDN>/callback` | Distribution domain + /callback |
| `VITE_COGNITO_LOGOUT_URI` | `https://<CDN>/` | Distribution domain |

## Step 7: Push to Deploy

```bash
git add .
git commit -m "feat: complete infrastructure setup"
git push origin main
```

Check **GitHub -> Actions** to see your pipeline running!

## Troubleshooting

See [ci-cd.md](ci-cd.md) for pipeline issues.
See [terraform.md](terraform.md) for infrastructure issues.
