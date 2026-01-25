variable "project_name" { type = string }
variable "environment" { type = string }
variable "instance_type" { type = string }
variable "key_name" { type = string }
variable "public_key_path" { type = string }
variable "vpc_id" { type = string }
variable "subnet_id" { type = string }
variable "iam_instance_profile" { type = string }
variable "rds_endpoint" { type = string }
variable "rds_db_name" { type = string }
variable "rds_username" { type = string }
variable "rds_password" {
  type      = string
  sensitive = true
}
variable "ecr_repository_url" { type = string }
variable "cognito_issuer_uri" { type = string }
variable "attachments_bucket_name" { type = string }
variable "ami_id" {
  description = "AMI ID for EC2 instance (Amazon Linux 2023 ARM in ap-southeast-2)"
  type        = string
  default     = "ami-0b8b26105f255e39d" # Amazon Linux 2023 ARM 64-bit in Sydney
}
