variable "project_name" { type = string }
variable "environment" { type = string }
variable "attachments_bucket_name" { type = string }
variable "frontend_bucket_name" { type = string }
variable "ec2_public_dns" {
  type        = string
  description = "EC2 public DNS for CloudFront API proxy"
}
