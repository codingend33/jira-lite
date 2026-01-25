# RDS Module - PostgreSQL Database

# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = var.subnet_ids

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  }
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-${var.environment}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from allowed security groups"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_sg_ids
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-rds-sg"
  }
}

# Parameter Group for PostgreSQL
resource "aws_db_parameter_group" "main" {
  name   = "${var.project_name}-${var.environment}-pg16"
  family = "postgres16"

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-pg16"
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-db"

  # Engine
  engine         = "postgres"
  engine_version = "16.6"

  # Instance
  instance_class    = var.instance_class
  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  # Database
  db_name  = var.db_name
  username = var.username
  password = var.password
  port     = 5432

  # Network
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Backup (disabled for Free Tier cost optimization)
  backup_retention_period = 0
  skip_final_snapshot     = true

  # Maintenance
  auto_minor_version_upgrade = true
  maintenance_window         = "sun:03:00-sun:04:00"

  # Monitoring
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  # Parameter group
  parameter_group_name = aws_db_parameter_group.main.name

  # Performance Insights (disabled for Free Tier)
  performance_insights_enabled = false

  tags = {
    Name = "${var.project_name}-${var.environment}-db"
  }
}
