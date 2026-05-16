output "app_url" {
  value = "https://${var.project}.volcanokun.dev"
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "codedeploy_app_name" {
  value = aws_codedeploy_app.main.name
}

output "codedeploy_deployment_group" {
  value = aws_codedeploy_deployment_group.main.deployment_group_name
}

output "github_actions_role_arn" {
  description = "GitHub Actions の AWS_ROLE_ARN シークレットに設定する値"
  value       = aws_iam_role.github_actions.arn
}

output "db_secret_arn" {
  value     = aws_secretsmanager_secret.db.arn
  sensitive = true
}
