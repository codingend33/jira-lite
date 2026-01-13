## Architecture

```mermaid
flowchart LR
  U[User] --> CF[CloudFront]
  CF --> S3FE[S3 Frontend]
  CF --> ALB[ALB]
  ALB --> ECS[ECS Fargate - Spring Boot]
  ECS --> RDS[(RDS Postgres)]
  ECS --> S3AT[S3 Attachments]
  ECS --> CW[CloudWatch Logs]
  U --> COG[Cognito]
  ECS --> COG
