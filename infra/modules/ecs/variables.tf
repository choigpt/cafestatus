variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "ecr_image" {
  type = string
}

variable "container_port" {
  type = number
}

variable "cpu" {
  type = number
}

variable "memory" {
  type = number
}

variable "desired_count" {
  type = number
}

variable "min_count" {
  type = number
}

variable "max_count" {
  type = number
}

variable "target_group_arn" {
  type = string
}

variable "alb_security_group_id" {
  type = string
}

# App environment
variable "db_host" {
  type = string
}

variable "db_port" {
  type = number
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_password_secret_arn" {
  type = string
}

variable "redis_host" {
  type = string
}

variable "redis_port" {
  type = number
}

variable "jwt_secret_arn" {
  type = string
}

variable "cors_allowed_origins" {
  type = string
}
