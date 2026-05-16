variable "aws_region" {
  type    = string
  default = "ap-northeast-1"
}

variable "project" {
  type    = string
  default = "metal-band-explorer"
}

variable "environment" {
  type    = string
  default = "prod"

  validation {
    condition     = contains(["prod", "stg", "dev"], var.environment)
    error_message = "environment must be one of: prod, stg, dev"
  }
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "availability_zones" {
  type    = list(string)
  default = ["ap-northeast-1a", "ap-northeast-1c"]
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "cloudflare_api_token" {
  type      = string
  sensitive = true
}

variable "github_org" {
  type = string
}

variable "github_repo" {
  type = string
}

variable "lastfm_api_key" {
  type      = string
  sensitive = true
}
