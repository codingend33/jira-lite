# Main Terraform configuration orchestrating all modules

# Network Module
module "network" {
  source = "./modules/network"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
}

# IAM Module (must be created before compute/lambda)
module "iam" {
  source = "./modules/iam"

  project_name          = var.project_name
  environment           = var.environment
  attachments_bucket_id = module.s3_cdn.attachments_bucket_id
  ecr_repository_arn    = module.ecr.repository_arn
  github_org            = var.github_org
  github_repo           = var.github_repo
  frontend_bucket_id    = module.s3_cdn.frontend_bucket_id
  cloudfront_dist_id    = module.s3_cdn.cloudfront_distribution_id
}

# ECR Module
module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
  environment  = var.environment
}

# RDS Module
module "rds" {
  source = "./modules/rds"

  project_name        = var.project_name
  environment         = var.environment
  instance_class      = var.rds_instance_class
  db_name             = var.rds_db_name
  username            = var.rds_username
  password            = var.rds_password
  allocated_storage   = var.rds_allocated_storage
  vpc_id              = module.network.vpc_id
  subnet_ids          = module.network.private_subnet_ids
  allowed_sg_ids      = [module.compute.security_group_id, module.network.lambda_security_group_id]
}

# S3 + CloudFront Module
module "s3_cdn" {
  source = "./modules/s3_cdn"

  project_name            = var.project_name
  environment             = var.environment
  attachments_bucket_name = var.attachments_bucket_name
  frontend_bucket_name    = var.frontend_bucket_name
  ec2_public_dns          = module.compute.public_dns
}

# Lambda Module (Pre Token Generation)
module "lambda" {
  source = "./modules/lambda"

  project_name           = var.project_name
  environment            = var.environment
  vpc_id                 = module.network.vpc_id
  subnet_ids             = module.network.private_subnet_ids
  security_group_id      = module.network.lambda_security_group_id
  db_host                = module.rds.address
  db_name                = var.rds_db_name
  db_user                = var.rds_username
  db_password            = var.rds_password
  user_pool_id           = var.cognito_user_pool_id
  lambda_role_arn        = module.iam.lambda_role_arn
}

# Compute Module (EC2)
module "compute" {
  source = "./modules/compute"

  project_name            = var.project_name
  environment             = var.environment
  instance_type           = var.ec2_instance_type
  key_name                = var.ec2_key_name
  public_key_path         = var.ec2_public_key_path
  vpc_id                  = module.network.vpc_id
  subnet_id               = module.network.public_subnet_ids[0]
  iam_instance_profile    = module.iam.ec2_instance_profile_name
  rds_endpoint            = module.rds.endpoint
  rds_db_name             = var.rds_db_name
  rds_username            = var.rds_username
  rds_password            = var.rds_password
  ecr_repository_url      = module.ecr.repository_url
  cognito_issuer_uri      = "https://cognito-idp.${var.aws_region}.amazonaws.com/${var.cognito_user_pool_id}"
  attachments_bucket_name = module.s3_cdn.attachments_bucket_name
  allowed_origins         = "*" # Set to "*" to avoid circular dependency with CloudFront domain
}

# CloudWatch Module
module "cloudwatch" {
  source = "./modules/cloudwatch"

  project_name = var.project_name
  environment  = var.environment
  ec2_instance_id = module.compute.instance_id
}
