resource "aws_ecr_repository" "ecr" {
  name                 = "${local.prefix}-ecr"
  image_tag_mutability = "MUTABLE"

  # I'll be tearing down most of the infrastructure to save money when I'm not using it, and this saves me from having
  # to manually delete my images.
  force_delete = true
  image_scanning_configuration {
    scan_on_push = false
  }

  # local-exec is pretty much always frowned upon, but I find it very clean for uploading containers for simple services.
  # In a real github flow deployment style microservice, we'd be deploying a new image with every run of the deployment
  # pipeline. We can run a simple bash script to build and upload the container as terraform is updating the infrastructure
  # instead of having to go back after the fact to update our task definitions.
  provisioner "local-exec" {
    working_dir = "../"
    command = "./build_and_upload.sh ${data.aws_caller_identity.env.account_id} us-east-1 ${aws_ecr_repository.ecr.name}"
  }
}

