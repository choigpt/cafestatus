project_name = "cafestatus"
environment  = "dev"
aws_region   = "ap-northeast-2"

# VPC
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["ap-northeast-2a", "ap-northeast-2c"]

# ECS
ecs_cpu           = 512
ecs_memory        = 1024
ecs_desired_count = 2
ecs_min_count     = 2
ecs_max_count     = 4
container_port    = 8080
ecr_image         = "ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/cafestatus:latest"

# RDS
db_instance_class = "db.t3.micro"
db_name           = "cafestatus"
db_username       = "admin"
# db_password     = (provide via CLI or env var TF_VAR_db_password)

# ElastiCache
redis_node_type = "cache.t3.micro"

# Secrets
# jwt_secret = (provide via CLI or env var TF_VAR_jwt_secret)

cors_allowed_origins = "*"
