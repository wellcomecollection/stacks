resource "aws_ecr_repository" "repository" {
  name = "uk.ac.wellcome/requests_api"

  lifecycle {
    prevent_destroy = true
  }
}