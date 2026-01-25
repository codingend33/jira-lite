terraform {
  backend "s3" {
    # IMPORTANT: Run infra/scripts/bootstrap-state.sh FIRST to create these resources
    # Then update the values below with your AWS account ID
    bucket         = "jira-lite-tf-state-854924710546"  # Replace with actual bucket name
    key            = "jira-lite/terraform.tfstate"
    region         = "ap-southeast-2"
    dynamodb_table = "jira-lite-tf-lock"
    encrypt        = true
  }
}
