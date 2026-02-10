# AWS Deployment Getting Started

This guide is for first-time deployment of the current Jira Lite stack.

## Prerequisites

- AWS account
- Terraform >= 1.6
- AWS CLI v2
- GitHub repository admin access
- Existing Cognito User Pool + App Client (this project expects existing Cognito values)

## 1) Configure AWS CLI

```bash
aws configure
aws sts get-caller-identity
```

Set region to `ap-southeast-2` unless your deployment uses another region.

## 2) Prepare Terraform Variables

```bash
cd infra/terraform
copy terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` and provide at minimum:

- `project_name`, `environment`
- `ec2_key_name`
- `rds_password`
- unique bucket names
- `cognito_user_pool_id`
- `cognito_client_id`
- `cognito_domain` (prefix only)
- `github_org`, `github_repo`

## 3) Bootstrap Terraform State Backend

```bash
cd ../scripts
./bootstrap-state.sh
```

Then update `infra/terraform/backend.tf` with the created S3 bucket and DynamoDB lock table.

## 4) Deploy Infrastructure

```bash
cd ../terraform
terraform init
terraform plan
terraform apply
```

Save outputs:

```bash
terraform output
```

Useful outputs:

- `github_actions_role_arn`
- `ecr_repository_url`
- `ec2_public_ip`
- `cloudfront_distribution_id`
- `cloudfront_domain_name`
- `cognito_auth_url`

## 5) Configure Cognito Trigger

In Cognito User Pool:

- Triggers -> Pre token generation -> select deployed Lambda from Terraform outputs (`lambda_function_name`).

If you use separate environments (recommended), use separate User Pools and separate trigger Lambda configuration per environment.

## 6) Configure GitHub Actions

### GitHub Secrets

- `AWS_ROLE_TO_ASSUME` = `terraform output github_actions_role_arn`
- `EC2_SSH_KEY` = PEM private key content for EC2 access

### GitHub Repository Variables

Core:

- `AWS_REGION` (for example `ap-southeast-2`)
- `ECR_REPOSITORY` = `terraform output ecr_repository_url`
- `EC2_HOST` = `terraform output ec2_public_ip`
- `FRONTEND_BUCKET` = `terraform output frontend_bucket_name`
- `CF_DIST_ID` = `terraform output cloudfront_distribution_id`

Frontend runtime:

- `VITE_API_BASE_URL` = `https://<cloudfront_domain>/api`
- `VITE_COGNITO_DOMAIN` = `https://<cognito_domain>.auth.<region>.amazoncognito.com`
- `VITE_COGNITO_CLIENT_ID` = Cognito app client id
- `VITE_COGNITO_REDIRECT_URI` = `https://<cloudfront_domain>/login`
- `VITE_COGNITO_LOGOUT_URI` = `https://<cloudfront_domain>/login`
- `VITE_COGNITO_SCOPE` = `openid email profile` (or include admin scope if required by your flow)

## 7) Trigger Deployment

Push to `main`:

```bash
git push origin main
```

Expected:

- Backend workflow runs `Test`, then `Build and Deploy` on main push.
- Frontend workflow runs build/test and deploys static assets + invalidates CloudFront.

## 8) Post-Deploy Verification

### API health

```bash
curl -i http://<EC2_PUBLIC_IP>:8080/api/health
```

### Frontend and API path routing

- Frontend: `https://<cloudfront_domain>`
- API via CloudFront: `https://<cloudfront_domain>/api/health`

### Swagger (direct backend)

- `http://<EC2_PUBLIC_IP>:8080/api/swagger-ui/index.html`

## 9) Environment Isolation Recommendation

For stable development and production consistency:

- Dev: Dev User Pool + Dev DB + Dev trigger Lambda
- Prod: Prod User Pool + Prod DB + Prod trigger Lambda

Avoid mixing "local/dev DB" with "prod User Pool" to prevent role/org claim drift.

## 10) Troubleshooting

- CI stuck on expected checks: see `docs/runbooks/ci-cd.md`
- Terraform issues/state locks: see `docs/runbooks/terraform.md`
- Local/runtime API issues: see `docs/runbooks/local-dev.md`
