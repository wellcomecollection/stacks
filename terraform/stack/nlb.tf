module "nlb" {
  source = "git::https://github.com/wellcometrust/terraform.git//load_balancer/network?ref=v14.2.0"

  namespace       = "${var.namespace}-stacks-api"
  private_subnets = "${var.private_subnets}"
}