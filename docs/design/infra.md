# Infrastructure Architecture

## Overview

Jira Lite infrastructure is deployed on AWS using Terraform for Infrastructure as Code (IaC). The architecture is optimized for AWS Free Tier while demonstrating production-ready practices.

## High-Level Architecture

```mermaid
flowchart TB
    subgraph Internet
        User[User Browser]
    end
    
    subgraph "AWS Cloud"
        subgraph "CloudFront+ S3"
            CF[CloudFront Distribution]
            S3FE[S3 Frontend Bucket]
        end
        
        subgraph "VPC 10.0.0.0/16"
            subgraph "Public Subnets"
                EC2[EC2 Backend<br/>t4g.micro]
                NAT[NAT Instance<br/>t4g.micro]
            end
            
            subgraph "Private Subnets"
                RDS[(RDS PostgreSQL<br/>db.t4g.micro)]
                Lambda[Lambda Pre Token]
            end
        end
        
        S3AT[S3 Attachments Bucket]
        ECR[ECR Backend Images]
        Cognito[Cognito User Pool]
        CW[CloudWatch Logs]
    end
    
    subgraph "CI/CD"
        GHA[GitHub Actions]
    end
    
    User -->|HTTPS| CF
    CF --> S3FE
    User -->|HTTP:8080| EC2
    EC2 --> RDS
    EC2 -->|Presigned URLs| S3AT
    EC2 --> CW
    EC2 --> Cognito
    Lambda --> RDS
    Cognito --> Lambda
    
    GHA -->|Push Image| ECR
    GHA -->|Deploy| EC2
    GHA -->|Sync| S3FE
    GHA -->|Invalidate| CF
    
    EC2 -->|Pull| ECR
    Lambda -.->|NAT| NAT
    NAT -->|IGW| Internet
