# General Configuration
variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "ap-southeast-2"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "jira-lite"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

# Network Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones for subnets"
  type        = list(string)
  default     = ["ap-southeast-2a", "ap-southeast-2b"]
}

# Compute Configuration
variable "ec2_instance_type" {
  description = "EC2 instance type (ARM-based for cost optimization)"
  type        = string
  default     = "t4g.micro"
}

variable "ec2_key_name" {
  description = "EC2 SSH key pair name (must exist in AWS or will be created)"
  type        = string
}

variable "ec2_public_key_path" {
  description = "Path to SSH public key file (optional, for creating new key pair)"
  type        = string
  default     = ""
}

# Database Configuration
variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_db_name" {
  description = "RDS database name"
  type        = string
  default     = "jira_lite"
}

variable "rds_username" {
  description = "RDS master username"
  type        = string
  default     = "jira_admin"
}

variable "rds_password" {
  description = "RDS master password (sensitive)"
  type        = string
  sensitive   = true
}

variable "rds_allocated_storage" {
  description = "RDS allocated storage in GB (max 20 for Free Tier)"
  type        = number
  default     = 20
}

# S3 Configuration
variable "attachments_bucket_name" {
  description = "S3 bucket name for attachments"
  type        = string
  default     = "amzn-jira1"
}

variable "frontend_bucket_name" {
  description = "S3 bucket name for frontend static files"
  type        = string
  default     = "amzn-jira-frontend"
}

# Cognito Configuration (Existing Resources)
variable "cognito_user_pool_id" {
  description = "Existing Cognito User Pool ID"
  type        = string
}

variable "cognito_client_id" {
  description = "Existing Cognito App Client ID"
  type        = string
}

variable "cognito_domain" {
  description = "Existing Cognito domain (without .auth.region.amazoncognito.com)"
  type        = string
}

# Lambda Configuration
variable "lambda_db_connections" {
  description = "Maximum database connections for Lambda"
  type        = number
  default     = 2
}

# GitHub OIDC Configuration
variable "github_org" {
  description = "GitHub organization or username"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name"
  type        = string
}

variable "alert_email" {
  description = "Email address for billing alerts"
  type        = string
}
