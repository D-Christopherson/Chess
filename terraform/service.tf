resource "aws_ecs_cluster" "cluster" {
  name = "${local.prefix}-cluster"
  configuration {
    execute_command_configuration {
      logging = "OVERRIDE"
      log_configuration {
        cloud_watch_encryption_enabled = true
        cloud_watch_log_group_name     = aws_cloudwatch_log_group.logs.name
      }
    }
  }
}

resource "aws_cloudwatch_log_group" "logs" {
  name              = "${local.prefix}-logs"
  retention_in_days = 1
}


resource "aws_ecs_task_definition" "service" {
  family = "service"
  cpu         = 256
  memory      = 512
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  task_role_arn            = aws_iam_role.service.arn
  execution_role_arn = aws_iam_role.cluster.arn

  container_definitions = jsonencode([
    {
      name        = "${local.prefix}-container"
      image       = "${aws_ecr_repository.ecr.repository_url}:latest"
      essential   = true
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group = aws_cloudwatch_log_group.logs.name
          awslogs-region = "us-east-1"
          awslogs-stream-prefix = "${local.prefix}-logs"
        }
      }
    }
  ])
}

resource "aws_security_group" "service-security-group" {
  description = "Allow inbound from ALB and all outbound"
  vpc_id = aws_default_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "allow_traffic_from_lb" {
  security_group_id = aws_security_group.service-security-group.id
  referenced_security_group_id = aws_security_group.lb-security-group.id
  from_port         = 8080
  ip_protocol       = "tcp"
  to_port           = 8080
}

resource "aws_vpc_security_group_egress_rule" "allow_service_outbound" {
  security_group_id = aws_security_group.service-security-group.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_ecs_service" "service" {
  name            = "${local.prefix}-service"
  cluster         = aws_ecs_cluster.cluster.arn
  task_definition = aws_ecs_task_definition.service.arn
  desired_count   = 1
  launch_type = "FARGATE"
  network_configuration {
    subnets = toset(data.aws_subnets.default.ids)
    # This is supposed to be frowned upon because then the cluster is reachable on the internet.
    # I find that it saves a lot of other configuration and my security group restricts inbound traffic to the ALB anyway.
    assign_public_ip = true
    security_groups = [aws_security_group.service-security-group.id]
  }
  # With the line above, Terraform will wait for the role to be created but not for the role policy
  # to be attached without the depends_on line.
  depends_on = [aws_iam_role_policy.cluster-policy]

  load_balancer {
    target_group_arn = aws_lb_target_group.target.arn
    container_name   = "${local.prefix}-container"
    container_port   = 8080

  }
}

data "aws_caller_identity" "env" {}


resource "aws_iam_role" "cluster" {
  assume_role_policy = <<JSON
    {
   "Version":"2012-10-17",
   "Statement":[
      {
         "Effect":"Allow",
         "Principal":{
            "Service":[
               "ecs.amazonaws.com",
               "ecs-tasks.amazonaws.com"
            ]
         },
         "Action":"sts:AssumeRole",
         "Condition":{
            "ArnLike":{
            "aws:SourceArn":"arn:aws:ecs:us-east-1:${data.aws_caller_identity.env.account_id}:*"
            },
            "StringEquals":{
               "aws:SourceAccount":"${data.aws_caller_identity.env.account_id}"
            }
         }
      }
   ]
}
JSON
}

# For the cluster itself. This is more management stuff like reading from ECR and creating/writing to log streams.
resource "aws_iam_role_policy" "cluster-policy" {
  role = aws_iam_role.cluster.id
  policy = <<JSON
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
              "ecr:BatchCheckLayerAvailability",
              "ecr:GetDownloadUrlForLayer",
              "ecr:BatchGetImage"
            ],
            "Resource": "${aws_ecr_repository.ecr.arn}*"
        },
        {
            "Effect": "Allow",
            "Action": [
              "ecr:GetAuthorizationToken"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
              "logs:CreateLogStream",
              "logs:PutLogEvents",
              "logs:DescribeLogStreams"
            ],
            "Resource": "${aws_cloudwatch_log_group.logs.arn}*"
        }
    ]
}
JSON
}

# The permissions that the containers running in the cluster will have. Nothing for now but if I wanted to use S3 or
# Dynamo, those permissions would go in a policy attached to this role.
resource "aws_iam_role" "service" {
  assume_role_policy = <<JSON
    {
   "Version":"2012-10-17",
   "Statement":[
      {
         "Effect":"Allow",
         "Principal":{
            "Service":[
               "ecs.amazonaws.com",
               "ecs-tasks.amazonaws.com"
            ]
         },
         "Action":"sts:AssumeRole",
         "Condition":{
            "ArnLike":{
            "aws:SourceArn":"arn:aws:ecs:us-east-1:${data.aws_caller_identity.env.account_id}:*"
            },
            "StringEquals":{
               "aws:SourceAccount":"${data.aws_caller_identity.env.account_id}"
            }
         }
      }
   ]
}
JSON
}