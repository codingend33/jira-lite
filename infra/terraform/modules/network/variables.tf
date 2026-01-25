variable "project_name" { type = string }
variable "environment" { type = string }
variable "vpc_cidr" { type = string }
variable "availability_zones" { type = list(string) }
variable "nat_ami_id" {
  description = "AMI ID for NAT Instance (AL2/AL2023 ARM)"
  type        = string
  default     = "ami-0b8b26105f255e39d" # Amazon Linux 2023 ARM 64-bit in Sydney
}
