locals {
    service_name = "chess"
    prefix = "${terraform.workspace}-${local.service_name}"
}