# ADR-0010: IaC with Terraform and EC2 Architecture

**Status**: Accepted  
**Date**: 2026-01-24  
**Deciders**: Development Team  

## Context

Need to deploy Jira Lite to AWS with fully reproducible infrastructure. Must optimize for Free Tier while demonstrating production-ready IaC practices.

## Decision

### 1. Use Terraform for Infrastructure as Code

**Chosen over**: Manual AWS Console configuration, CloudFormation, CDK

**Reasons**:
- Industry-standard tool (multi-cloud, widely adopted)
- Declarative syntax easier to read/review than CloudFormation JSON
- State management enables team collaboration
- Modular design supports code reuse

### 2. EC2-based Architecture

**Chosen over**: ECS Fargate, EKS, Elastic Beanstalk

**Reasons**:
- **Free Tier**: 750 hours/month of t4g.micro (ARM) EC2
- **Simplicity**: Single instance sufficient for demo/portfolio
- **Docker-ready**: Run containerized app without ECS complexity
- **Cost**: $0 vs ~$30/month minimum for Fargate

### 3. NAT Instance instead of NAT Gateway

**Chosen over**: NAT Gateway

**Reasons**:
- **Free Tier**: t4g.micro NAT instance = $0
- **Cost avoidance**: NAT Gateway = $32+/month
- **Trade-off**: Lower throughput acceptable for demo workload

### 4. Single-AZ Deployment

**Chosen over**: Multi-AZ high availability

**Reasons**:
- **Portfolio project**: HA not required for demonstration
- **Free Tier**: Multi-AZ RDS doubles cost
- **Acceptable downtime**: Not production-critical

## Consequences

### Positive
- ✅ 100% IaC - fully reproducible
- ✅ Free Tier compliant (~$0/month)
- ✅ Simple architecture easy to understand
- ✅ Git-tracked infrastructure changes

### Negative
- ❌ Single point of failure (no HA)
- ❌ Manual scaling (not auto-scaling)
- ❌ NAT Instance lower performance than Gateway

### Neutral
- Manual Cognito configuration (existing resource referenced)
- Lambda Pre Token Generation added for org_id injection

## Implementation Notes

- Terraform state stored in S3 + DynamoDB (bootstrap script)
- Modular structure: network, compute, rds, s3_cdn, iam, lambda
- Free Tier optimizations: disabled RDS backups, 7-day log retention
