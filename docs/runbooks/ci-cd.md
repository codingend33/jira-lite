# CI/CD Pipeline Guide

This guide covers GitHub Actions CI/CD pipeline usage and troubleshooting.

## Pipeline Overview

### Backend Pipeline (`.github/workflows/backend.yml`)

**Trigger**: Push to `main` branch with changes in `backend/**`

**Steps**:
1. Test (Maven)
2. Build JAR
3. Build Docker image
4. Push to ECR
5. SSH to EC2 and deploy

**Duration**: ~5-8 minutes

### Frontend Pipeline (`.github/workflows/frontend.yml`)

**Trigger**: Push to `main` branch with changes in `frontend/**`

**Steps**:
1. Build (Vite)
2. Sync to S3
3. Invalidate CloudFront cache

**Duration**: ~3-5 minutes

## Manual Triggering

Both pipelines support manual dispatch:

1. Go to GitHub Actions tab
2. Select workflow (Backend CI/CD or Frontend CI/CD)
3. Click "Run workflow"
4. Select branch and run

## Monitoring Deployments

### GitHub Actions UI

- **Summary**: View deployment summary with URLs
- **Logs**: Click on each step to view detailed logs
- **Artifacts**: Download test reports if tests fail

### Health Check

Backend deployment includes automatic health check:

```bash
curl http://<EC2_IP>:8080/health
```

Expected response: `{"status":"UP"}`

### CloudFront Status

Check CloudFront invalidation progress:

```bash
aws cloudfront list-invalidations --distribution-id <CF_DIST_ID>
```

## Common Issues

### 1. ECR Push Failed - Unauthorized

**Symptom**: `denied: Your authorization token has expired`

**Cause**: OIDC role doesn't have ECR permissions

**Fix**:
```bash
# Verify OIDC trust policy
aws iam get-role --role-name jira-lite-prod-github-actions-role

# Check ECR policy attached
aws iam list-role-policies --role-name jira-lite-prod-github-actions-role
```

### 2. SSH Connection Failed

**Symptom**: `Permission denied (publickey)`

**Causes**:
- Wrong SSH key in GitHub Secrets
- Security group blocking port 22
- EC2 instance not running

**Fix**:
```bash
# Test SSH manually
ssh -i your-key.pem ec2-user@<EC2_IP>

# Check security group allows your IP
aws ec2 describe-security-groups --group-ids <SG_ID>

# Verify EC2 status
aws ec2 describe-instances --instance-ids <INSTANCE_ID>
```

### 3. EC2 Deployment Health Check Failed

**Symptom**: `Health check failed!` in deployment logs

**Causes**:
- Application failed to start
- Port 8080 not listening
- Database connection failed

**Debug**:
```bash
# SSH to EC2
ssh -i your-key.pem ec2-user@<EC2_IP>

# Check container logs
docker logs jira-backend

# Check container status
docker ps -a

# Test health endpoint locally
curl http://localhost:8080/health

# Check database connectivity
docker exec jira-backend nc -zv <RDS_ENDPOINT> 5432
```

### 4. S3 Sync Failed - Access Denied

**Symptom**: `AccessDenied` when syncing to S3

**Cause**: OIDC role missing S3 permissions

**Fix**:
```bash
# Check S3 policy
aws iam get-role-policy \
  --role-name jira-lite-prod-github-actions-role \
  --policy-name s3-sync
```

### 5. CloudFront Invalidation Failed

**Symptom**: `InvalidationBatchAlreadyExists` or timeout

**Cause**: Previous invalidation still in progress

**Fix**:
```bash
# List invalidations
aws cloudfront list-invalidations --distribution-id <CF_DIST_ID>

# Wait for completion (max 15 minutes)
aws cloudfront wait invalidation-completed \
  --distribution-id <CF_DIST_ID> \
  --id <INVALIDATION_ID>
```

### 6. Frontend CORS Errors

**Symptom**: Browser console shows CORS errors when accessing API

**Cause**: API URL mismatch or Cognito redirect URI misconfigured

**Fix**:
1. Verify `VITE_API_BASE_URL` points to correct EC2 IP
2. Check backend CORS configuration in `SecurityConfig.java`
3. Verify Cognito redirect URIs include CloudFront domain

### 7. Docker Image Too Large

**Symptom**: ECR push timeout or slow deployment

**Optimization**:
```dockerfile
# Use multi-stage build
FROM eclipse-temurin:17-jdk-alpine AS build
# ... build steps

FROM eclipse-temurin:17-jre-alpine
# Only copy JAR, not build dependencies
```

## Rollback Procedure

### Backend Rollback

```bash
# SSH to EC2
ssh -i your-key.pem ec2-user@<EC2_IP>

# Find previous image tag
aws ecr describe-images \
  --repository-name jira-lite-backend \
  --query 'sort_by(imageDetails, &imagePushedAt)[-5:]'

# Pull previous version
docker pull <ECR_URL>:<PREVIOUS_SHA>

# Stop current container
docker stop jira-backend
docker rm jira-backend

# Run previous version
docker run -d \
  --name jira-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file ~/.env \
  <ECR_URL>:<PREVIOUS_SHA>
```

### Frontend Rollback

```bash
# Restore from S3 versioning (if enabled)
aws s3api list-object-versions \
  --bucket <FRONTEND_BUCKET> \
  --prefix index.html

# Or redeploy from previous commit
git checkout <PREVIOUS_COMMIT>
cd frontend
npm ci && npm run build
aws s3 sync dist/ s3://<FRONTEND_BUCKET> --delete
aws cloudfront create-invalidation --distribution-id <CF_DIST_ID> --paths "/*"
```

## Performance Tips

### Speed Up Docker Builds

- Use Docker layer caching in GitHub Actions
- Optimize Dockerfile layer order (least → most frequently changed)

### Reduce S3 Sync Time

- Use `--size-only` flag for faster comparison
- Only invalidate changed paths in CloudFront

## Security Best Practices

- ✅ Never commit SSH keys or AWS credentials
- ✅ Rotate EC2 SSH keys regularly
- ✅ Review CloudTrail logs for OIDC role usage
- ✅ Enable branch protection on `main`
- ✅ Require PR reviews before merge
