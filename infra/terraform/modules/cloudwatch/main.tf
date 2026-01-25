# CloudWatch Module - Logging and Monitoring

# Log Group for EC2 application logs
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/aws/ec2/${var.project_name}-${var.environment}-backend"
  retention_in_days = 7  # Free Tier: 5GB storage, keep 7 days

  tags = {
    Name = "${var.project_name}-${var.environment}-backend-logs"
  }
}

# Optional: CPU Utilization Alarm
resource "aws_cloudwatch_metric_alarm" "cpu" {
  alarm_name          = "${var.project_name}-${var.environment}-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80

  dimensions = {
    InstanceId = var.ec2_instance_id
  }

  alarm_description = "Alert when EC2 CPU exceeds 80%"
  treat_missing_data = "notBreaching"

  tags = {
    Name = "${var.project_name}-${var.environment}-cpu-alarm"
  }
}
