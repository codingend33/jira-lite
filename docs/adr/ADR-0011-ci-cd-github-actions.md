# ADR-0011: CI/CD with GitHub Actions

**Status**: Accepted  
**Date**: 2026-01-24  
**Deciders**: Development Team  

## Context

Need automated CI/CD pipeline for backend (Java/Docker) and frontend (React/Vite) deployment to AWS infrastructure.

## Decision

### 1. GitHub Actions for CI/CD

**Chosen over**: Jenkins, GitLab CI, CircleCI

**Reasons**:
- **Native integration**: Already using GitHub for code
- **Free Tier**: 2000 minutes/month (sufficient for project)
- **OIDC support**: Secure AWS authentication without long-lived credentials
- **Marketplace**: Rich ecosystem of reusable actions

### 2. OIDC for AWS Authentication

**Chosen over**: IAM access keys as secrets

**Reasons**:
- **Security**: No long-lived credentials to rotate
- **Best practice**: AWS-recommended approach
- **Least privilege**: Role assumed only during workflow execution
- **Audit trail**: CloudTrail logs show OIDC-based access

### 3. SSH Deployment to EC2

**Chosen over**: CodeDeploy, SSM Session Manager

**Reasons**:
- **Simplicity**: Direct SSH for single-instance deployment
- **Control**: Full visibility into deployment steps
- **Debugging**: Easy to SSH manually for troubleshooting
- **Free**: No CodeDeploy agent overhead

### 4. Separate Backend/Frontend Pipelines

**Triggered by**: Path-based filters (`backend/**`, `frontend/**`)

**Reasons**:
- **Efficiency**: Don't rebuild frontend on backend changes
- **Parallelization**: Independent deployments
- **Clear separation**: Different deployment targets (EC2 vs S3/CloudFront)

## Architecture

### Backend Pipeline
```
Test → Build JAR → Docker Build → ECR Push → SSH to EC2 → Docker Pull → Deploy
```

### Frontend Pipeline
```
Build (Vite) → S3 Sync → CloudFront Invalidation
```

## Consequences

### Positive
- ✅ Fully automated deployment
- ✅ Secure OIDC authentication
- ✅ Fast feedback (5-10 min total)
- ✅ Git history = deployment audit trail

### Negative
- ❌ No blue-green deployment (downtime during EC2 restart)
- ❌ Manual rollback (SSH + redeploy old tag)

### Neutral
- Health check validates deployment success
- CloudFront invalidation ensures cache freshness

## Security Considerations

- **Secrets**: SSH private key stored in GitHub Secrets (encrypted)
- **OIDC**: Role ARN trusted only for specific repo
- **Least privilege**: IAM policies scoped to specific resources
- **No plaintext**: All sensitive values via Secrets/Variables
