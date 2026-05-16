# ECS Task Definition (Batch — 別タスク定義、APIとは分離)

resource "aws_ecs_task_definition" "batch" {
  family                   = "${local.name_prefix}-batch"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 1024
  memory                   = 2048
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "batch"
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true

      environment = [
        { name = "JAVA_TOOL_OPTIONS", value = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0" },
        { name = "SPRING_PROFILES_ACTIVE", value = "batch" },
        { name = "CLOUDWATCH_ENABLED", value = "true" },
        { name = "AWS_REGION", value = var.aws_region },
      ]

      secrets = [
        {
          name      = "DB_URL"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:db_url::"
        },
        {
          name      = "DB_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:username::"
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:password::"
        },
        {
          name      = "LASTFM_API_KEY"
          valueFrom = aws_secretsmanager_secret.lastfm.arn
        },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.batch.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "batch"
        }
      }
    }
  ])

  tags = { Name = "${local.name_prefix}-batch-task-def" }
}

resource "aws_cloudwatch_log_group" "batch" {
  name              = "/ecs/${local.name_prefix}-batch"
  retention_in_days = 30

  tags = { Name = "${local.name_prefix}-batch-logs" }
}

# EventBridge Scheduler IAM Role

resource "aws_iam_role" "eventbridge_scheduler" {
  name = "${local.name_prefix}-scheduler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "scheduler.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = { Name = "${local.name_prefix}-scheduler-role" }
}

resource "aws_iam_role_policy" "eventbridge_scheduler" {
  name = "${local.name_prefix}-scheduler-policy"
  role = aws_iam_role.eventbridge_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["ecs:RunTask"]
      Resource = [aws_ecs_task_definition.batch.arn]
      Condition = {
        ArnLike = {
          "ecs:cluster" = aws_ecs_cluster.main.arn
        }
      }
    },
    {
      Effect   = "Allow"
      Action   = ["iam:PassRole"]
      Resource = [
        aws_iam_role.ecs_execution.arn,
        aws_iam_role.ecs_task.arn,
      ]
    }]
  })
}

# EventBridge Scheduler: 毎日 02:00 JST (17:00 UTC)

resource "aws_scheduler_schedule" "batch_daily" {
  name       = "${local.name_prefix}-batch-daily"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression          = "cron(0 17 * * ? *)"
  schedule_expression_timezone = "UTC"

  target {
    arn      = aws_ecs_cluster.main.arn
    role_arn = aws_iam_role.eventbridge_scheduler.arn

    ecs_parameters {
      task_definition_arn = aws_ecs_task_definition.batch.arn
      launch_type         = "FARGATE"
      task_count          = 1

      network_configuration {
        subnets          = aws_subnet.private[*].id
        security_groups  = [aws_security_group.ecs.id]
        assign_public_ip = false
      }
    }

    retry_policy {
      maximum_retry_attempts = 1
    }
  }

  tags = { Name = "${local.name_prefix}-batch-daily" }
}

# SNS Topic for batch failure notifications

resource "aws_sns_topic" "batch_alerts" {
  name = "${local.name_prefix}-batch-alerts"

  tags = { Name = "${local.name_prefix}-batch-alerts" }
}

# CloudWatch Alarm: batch.job.status > 0 (failure)

resource "aws_cloudwatch_metric_alarm" "batch_failure" {
  alarm_name          = "${local.name_prefix}-batch-failure"
  alarm_description   = "Artist Discovery Batch job failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "batch.job.status"
  namespace           = "MetalExplorer"
  period              = 3600
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    job = "artistDiscoveryJob"
  }

  alarm_actions             = [aws_sns_topic.batch_alerts.arn]
  insufficient_data_actions = []

  tags = { Name = "${local.name_prefix}-batch-failure-alarm" }
}
