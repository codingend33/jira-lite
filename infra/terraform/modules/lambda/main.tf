# Lambda Module - Pre Token Generation Function

# Package Lambda function code
data "archive_file" "lambda" {
  type        = "zip"
  source_dir  = "${path.root}/../lambda/pre-token-generation"
  output_path = "${path.module}/lambda.zip"
  
  excludes = ["build.sh", "lambda.zip", "__pycache__", "*.pyc"]
}

# Lambda Function
resource "aws_lambda_function" "pre_token" {
  filename      = data.archive_file.lambda.output_path
  function_name = "${var.project_name}-${var.environment}-pre-token-generation"
  role          = var.lambda_role_arn
  handler       = "index.handler"
  runtime       = "python3.12"
  timeout       = 10
  memory_size   = 256

  layers = ["arn:aws:lambda:ap-southeast-2:770693421928:layer:Klayers-p312-psycopg2-binary:1"]

  source_code_hash = data.archive_file.lambda.output_base64sha256

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [var.security_group_id]
  }

  environment {
    variables = {
      DB_HOST     = var.db_host
      DB_NAME     = var.db_name
      DB_USER     = var.db_user
      DB_PASSWORD = var.db_password
    }
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-pre-token-lambda"
  }
}

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${aws_lambda_function.pre_token.function_name}"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-lambda-logs"
  }
}

# Lambda Permission for Cognito to invoke
resource "aws_lambda_permission" "cognito" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.pre_token.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = "arn:aws:cognito-idp:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:userpool/${var.user_pool_id}"
}

# Update Cognito User Pool with Lambda trigger
# resource "aws_cognito_user_pool" "update_trigger" {
#   # This is a workaround to update existing User Pool with Lambda trigger
#   # Note: Requires import of existing User Pool or will create new one
#   
#   # IMPORTANT: Comment out this resource if using existing Cognito
#   # Instead, manually configure Lambda trigger in AWS Console:
#   # Cognito User Pool → Triggers → Pre token generation → Select Lambda function
#   
#   count = 0  # Disabled - configure manually for existing User Pool
# }

data "aws_region" "current" {}
data "aws_caller_identity" "current" {}
