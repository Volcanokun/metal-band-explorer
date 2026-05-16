resource "random_password" "db" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "${local.name_prefix}-db-subnet-group" }
}

resource "aws_rds_cluster_parameter_group" "main" {
  name        = "${local.name_prefix}-cluster-pg"
  family      = "aurora-postgresql16"
  description = "Aurora PostgreSQL 16 cluster parameter group"

  tags = { Name = "${local.name_prefix}-cluster-pg" }
}

resource "aws_rds_cluster" "main" {
  cluster_identifier        = "${local.name_prefix}-cluster"
  engine                    = "aurora-postgresql"
  engine_mode               = "provisioned"
  engine_version            = "16.6"
  database_name             = "metalexplorer"
  master_username           = "metaladmin"
  master_password           = random_password.db.result
  db_subnet_group_name      = aws_db_subnet_group.main.name
  vpc_security_group_ids    = [aws_security_group.rds.id]
  db_cluster_parameter_group_name = aws_rds_cluster_parameter_group.main.name

  serverlessv2_scaling_configuration {
    min_capacity = 0.5
    max_capacity = 4.0
  }

  skip_final_snapshot     = false
  final_snapshot_identifier = "${local.name_prefix}-final-snapshot"
  deletion_protection     = true
  storage_encrypted       = true
  backup_retention_period = 7

  tags = { Name = "${local.name_prefix}-cluster" }
}

resource "aws_rds_cluster_instance" "main" {
  identifier         = "${local.name_prefix}-instance-1"
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.main.engine
  engine_version     = aws_rds_cluster.main.engine_version

  tags = { Name = "${local.name_prefix}-instance-1" }
}

# Secrets Manager: DB credentials + JDBC URL

resource "aws_secretsmanager_secret" "db" {
  name                    = "${local.name_prefix}/db"
  description             = "Aurora credentials and JDBC URL"
  recovery_window_in_days = 7

  tags = { Name = "${local.name_prefix}-db-secret" }
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = aws_rds_cluster.main.master_username
    password = random_password.db.result
    host     = aws_rds_cluster.main.endpoint
    port     = 5432
    dbname   = aws_rds_cluster.main.database_name
    db_url   = "jdbc:postgresql://${aws_rds_cluster.main.endpoint}:5432/${aws_rds_cluster.main.database_name}"
  })
}

# Secrets Manager: Last.fm API key (plain string)

resource "aws_secretsmanager_secret" "lastfm" {
  name                    = "${local.name_prefix}/lastfm-api-key"
  description             = "Last.fm API key"
  recovery_window_in_days = 7

  tags = { Name = "${local.name_prefix}-lastfm-secret" }
}

resource "aws_secretsmanager_secret_version" "lastfm" {
  secret_id     = aws_secretsmanager_secret.lastfm.id
  secret_string = var.lastfm_api_key
}
