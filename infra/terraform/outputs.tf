# VPC Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = module.network.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.network.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.network.private_subnet_ids
}

# EC2 Outputs
output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = module.compute.instance_id
}

output "ec2_public_ip" {
  description = "EC2 public IP address"
  value       = module.compute.public_ip
}

output "ec2_public_dns" {
  description = "EC2 public DNS name"
  value       = module.compute.public_dns
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.endpoint
}

output "rds_address" {
  description = "RDS address (hostname only)"
  value       = module.rds.address
}

output "rds_database_name" {
  description = "RDS database name"
  value       = module.rds.database_name
}

# ECR Outputs
output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = module.ecr.repository_url
}

output "ecr_repository_arn" {
  description = "ECR repository ARN"
  value       = module.ecr.repository_arn
}

# S3 Outputs
output "attachments_bucket_name" {
  description = "Attachments S3 bucket name"
  value       = module.s3_cdn.attachments_bucket_name
}

output "frontend_bucket_name" {
  description = "Frontend S3 bucket name"
  value       = module.s3_cdn.frontend_bucket_name
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID"
  value       = module.s3_cdn.cloudfront_distribution_id
}

output "cloudfront_domain_name" {
  description = "CloudFront domain name"
  value       = module.s3_cdn.cloudfront_domain_name
}

# Cognito Outputs
output "cognito_issuer_uri" {
  description = "Cognito issuer URI for JWT validation"
  value       = "https://cognito-idp.${var.aws_region}.amazonaws.com/${var.cognito_user_pool_id}"
}

output "cognito_auth_url" {
  description = "Cognito authorization URL"
  value       = "https://${var.cognito_domain}.auth.${var.aws_region}.amazoncognito.com"
}

# Lambda Outputs
output "lambda_function_name" {
  description = "Pre Token Generation Lambda function name"
  value       = module.lambda.function_name
}

output "lambda_function_arn" {
  description = "Pre Token Generation Lambda function ARN"
  value       = module.lambda.function_arn
}

# IAM Outputs
output "ec2_instance_profile_name" {
  description = "EC2 instance profile name"
  value       = module.iam.ec2_instance_profile_name
}

output "github_actions_role_arn" {
  description = "GitHub Actions OIDC role ARN"
  value       = module.iam.github_actions_role_arn
}

# Summary Output
output "deployment_summary" {
  description = "Summary of deployed resources for CI/CD"
  value = {
    backend_api_url        = "http://${module.compute.public_ip}:8080"
    frontend_url           = "https://${module.s3_cdn.cloudfront_domain_name}"
    ecr_repository         = module.ecr.repository_url
    rds_connection_string  = "postgresql://${var.rds_username}@${module.rds.address}:5432/${var.rds_db_name}"
    cloudfront_dist_id     = module.s3_cdn.cloudfront_distribution_id
    github_actions_role    = module.iam.github_actions_role_arn
  }
}
