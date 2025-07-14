# The load balancer probably belongs in a separate project in case I want multiple services running on
# my domain, but I'll move it if that ever happens.

resource "aws_security_group" "lb-security-group" {
  description = "Allow inbound tls and all outbound"
  vpc_id = aws_default_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "allow_tls_ipv4" {
  security_group_id = aws_security_group.lb-security-group.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  ip_protocol       = "tcp"
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "allow_all_ipv4" {
  security_group_id = aws_security_group.lb-security-group.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_lb" "load_balancer" {
  name               = "${local.prefix}-load-balancer"
  internal           = false
  load_balancer_type = "application"
  subnets            = toset(data.aws_subnets.default.ids)
  security_groups = [aws_security_group.lb-security-group.id]
}

resource "aws_lb_listener" "alb" {
  load_balancer_arn = aws_lb.load_balancer.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = data.aws_acm_certificate.cert.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.target.arn
  }
}

resource "aws_lb_target_group" "target" {
  name        = "${local.prefix}-target-group"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_default_vpc.default.id
  health_check {
    path = "/actuator/health"
  }
}

resource "aws_default_vpc" "default" {

}

data "aws_subnets" "default" {
  filter {
    name = "vpc-id"
    values = [aws_default_vpc.default.id]
  }
}

# The domain and cert are managed outside of this terraform so I can tear down everything else to save money when not in
# use.
data "aws_acm_certificate" "cert" {
  domain = "*.dakotachristopherson.com"
}

resource "aws_route53_zone" "chess" {
  name = "chess.dakotachristopherson.com"
}

# Buying the domain seems to have given me this zone automatically
data "aws_route53_zone" "default" {
  name = "dakotachristopherson.com"
}

# First we have to tell the apex name server about our subdomain's name server
resource "aws_route53_record" "chess-ns" {
  zone_id = data.aws_route53_zone.default.zone_id
  name = "chess"
  type = "NS"
  ttl = "86400"
  records = toset(aws_route53_zone.chess.name_servers)
}

# Then the subdomain gets its actual record that points at the LB
resource "aws_route53_record" "chess-dns" {
  zone_id = aws_route53_zone.chess.id
  name    = "chess.dakotachristopherson.com"
  type    = "A"

  alias {
    name = aws_lb.load_balancer.dns_name
    zone_id = aws_lb.load_balancer.zone_id
    evaluate_target_health = true
  }
}