output "ec2_instance_profile_name" {
  description = "EC2 instance profile name"
  value       = aws_iam_instance_profile.ec2.name
}

output "ec2_role_arn" {
  description = "EC2 role ARN"
  value       = aws_iam_role.ec2.arn
}

output "lambda_role_arn" {
  description = "Lambda role ARN"
  value       = aws_iam_role.lambda.arn
}

output "github_actions_role_arn" {
  description = "GitHub Actions role ARN"
  value       = aws_iam_role.github_actions.arn
}
