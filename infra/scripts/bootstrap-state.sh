#!/bin/bash
# Bootstrap Terraform remote state backend
# Run this ONCE before first terraform init

set -e

REGION="ap-southeast-2"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
STATE_BUCKET="jira-lite-tf-state-${ACCOUNT_ID}"
LOCK_TABLE="jira-lite-tf-lock"

echo "🚀 Bootstrapping Terraform state backend..."
echo "   Region: ${REGION}"
echo "   Account: ${ACCOUNT_ID}"
echo "   Bucket: ${STATE_BUCKET}"
echo "   Lock Table: ${LOCK_TABLE}"

# Create S3 bucket for state
echo "📦 Creating S3 bucket..."
aws s3api create-bucket \
  --bucket "${STATE_BUCKET}" \
  --region "${REGION}" \
  --create-bucket-configuration LocationConstraint="${REGION}" \
  2>/dev/null || echo "   ℹ️  Bucket already exists"

# Enable versioning
echo "🔄 Enabling versioning..."
aws s3api put-bucket-versioning \
  --bucket "${STATE_BUCKET}" \
  --versioning-configuration Status=Enabled

# Enable encryption
echo "🔒 Enabling encryption..."
aws s3api put-bucket-encryption \
  --bucket "${STATE_BUCKET}" \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
echo "🛡️  Blocking public access..."
aws s3api put-public-access-block \
  --bucket "${STATE_BUCKET}" \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Create DynamoDB table for state locking
echo "🔐 Creating DynamoDB lock table..."
aws dynamodb create-table \
  --table-name "${LOCK_TABLE}" \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "${REGION}" \
  2>/dev/null || echo "   ℹ️  Table already exists"

echo ""
echo "✅ Bootstrap complete!"
echo ""
echo "📝 Update infra/terraform/backend.tf with:"
echo "   bucket = \"${STATE_BUCKET}\""
echo "   dynamodb_table = \"${LOCK_TABLE}\""
echo ""
