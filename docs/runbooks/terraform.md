# Terraform Reference

> **New Users**: Read [AWS Deployment Getting Started](getting-started-aws.md) first.
>
> This document is a reference for advanced Terraform operations after the initial deployment.

## Daily Operations

### Check Current State

```bash
cd infra/terraform
terraform show
```

### View Outputs

```bash
terraform output
terraform output ec2_public_ip
terraform output cloudfront_domain_name
```

## Updating Infrastructure

### Modifying Configuration

1. Edit `terraform.tfvars` or `.tf` files.
2. Preview changes:
   ```bash
   terraform plan
   ```
3. Apply changes:
   ```bash
   terraform apply
   ```

### Common Scenarios

**Change EC2 Instance Type**:
```hcl
# terraform.tfvars
ec2_instance_type = "t4g.small"  # Upgrade from t4g.micro
```

**Rotate RDS Password**:
```hcl
# terraform.tfvars
rds_password = "NewSecurePassword456!@#"
```

## Destroying Infrastructure

> ⚠️ **WARNING**: This deletes ALL resources and data permanently!

```bash
terraform destroy
```

Type `yes` to confirm.

### Destroy Specific Resource

```bash
terraform destroy -target=module.compute
```

## Troubleshooting

### State Lock Error

If Terraform hangs with a state lock error:

```bash
# List locks
aws dynamodb scan --table-name jira-lite-tf-lock

# Force unlock (use Lock ID from error message)
terraform force-unlock <LOCK_ID>
```

### Backend Connection Failed

Check `backend.tf`:
- Correct bucket name?
- Correct region (`ap-southeast-2`)?
- Does DynamoDB table exist?

## Free Tier Monitoring

Current configuration usage vs Free Tier limits:

| Resource | Config | Free Tier Limit |
|----------|--------|-----------------|
| EC2 | t4g.micro | 750 hours/mo |
| RDS | db.t4g.micro | 750 hours/mo |
| NAT | t4g.micro | 750 hours/mo |
| S3 | Standard | 5GB Storage |
| CloudWatch | 7 days | 5GB Data |

**Est. Cost**: ~$0-5/month (mostly NAT data transfer)
