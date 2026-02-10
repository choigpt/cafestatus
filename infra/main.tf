terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Uncomment and configure for remote state
  # backend "s3" {
  #   bucket         = "cafestatus-terraform-state"
  #   key            = "terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "terraform-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# --- VPC ---
module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
}

# --- Secrets Manager ---
resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${var.project_name}-${var.environment}-jwt-secret"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.project_name}-${var.environment}-db-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

# --- RDS MySQL ---
module "rds" {
  source = "./modules/rds"

  project_name    = var.project_name
  environment     = var.environment
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  instance_class  = var.db_instance_class
  db_name         = var.db_name
  db_username     = var.db_username
  db_password     = var.db_password
  app_security_group_id = module.ecs.app_security_group_id
}

# --- ElastiCache Redis ---
module "elasticache" {
  source = "./modules/elasticache"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  subnet_ids            = module.vpc.private_subnet_ids
  node_type             = var.redis_node_type
  app_security_group_id = module.ecs.app_security_group_id
}

# --- ALB ---
module "alb" {
  source = "./modules/alb"

  project_name   = var.project_name
  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.public_subnet_ids
  container_port = var.container_port
}

# --- ECS Fargate ---
module "ecs" {
  source = "./modules/ecs"

  project_name         = var.project_name
  environment          = var.environment
  aws_region           = var.aws_region
  vpc_id               = module.vpc.vpc_id
  subnet_ids           = module.vpc.private_subnet_ids
  ecr_image            = var.ecr_image
  container_port       = var.container_port
  cpu                  = var.ecs_cpu
  memory               = var.ecs_memory
  desired_count        = var.ecs_desired_count
  min_count            = var.ecs_min_count
  max_count            = var.ecs_max_count
  target_group_arn     = module.alb.target_group_arn
  alb_security_group_id = module.alb.security_group_id

  # App environment
  db_host              = module.rds.endpoint
  db_port              = module.rds.port
  db_name              = var.db_name
  db_username          = var.db_username
  db_password_secret_arn = aws_secretsmanager_secret.db_password.arn
  redis_host           = module.elasticache.endpoint
  redis_port           = module.elasticache.port
  jwt_secret_arn       = aws_secretsmanager_secret.jwt_secret.arn
  cors_allowed_origins = var.cors_allowed_origins
}
