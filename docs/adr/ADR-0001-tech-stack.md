# ADR-0001: Technology Stack

## Status
Accepted

## Context
Jira Lite is a production-style portfolio project focusing on multi-tenancy, RBAC, CI/CD and AWS deployment.

## Decision
- Backend: Java 17 + Spring Boot 3 + Spring Security + JPA/Hibernate + Flyway + OpenAPI
- Frontend: React + TypeScript + React Router + React Query + MUI
- Infra: AWS (Cognito/S3/RDS/ECS/ALB/CloudWatch/ECR/CloudFront) + Terraform
- CI/CD: GitHub Actions

## Consequences
- Aligns with common enterprise stack in Australia
- Clear layering and maintainability
- Higher setup overhead but stronger portfolio signal
