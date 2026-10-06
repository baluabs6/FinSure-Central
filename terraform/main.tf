terraform {
  required_providers {
    azurerm = { source = "hashicorp/azurerm", version = "~> 4.0" }
    aws     = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}
provider "azurerm" { features {} }
provider "aws" { region = var.dr_region }

variable "prefix"    { default = "finsure" }
variable "location"  { default = "centralindia" }
variable "dr_region" { default = "ap-south-1" }
variable "subscription_id" {}

resource "azurerm_resource_group" "rg" { name = "${var.prefix}-rg" location = var.location }
resource "azurerm_container_registry" "acr" {
  name = "${var.prefix}acr" resource_group_name = azurerm_resource_group.rg.name location = var.location sku = "Standard"
}
resource "azurerm_kubernetes_cluster" "aks" {
  name = "${var.prefix}-aks" location = var.location resource_group_name = azurerm_resource_group.rg.name dns_prefix = var.prefix
  default_node_pool { name = "default" node_count = 2 vm_size = "Standard_D2s_v3" }
  identity { type = "SystemAssigned" }
}
resource "azurerm_role_assignment" "aks_acr" {
  principal_id = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
  role_definition_name = "AcrPull" scope = azurerm_container_registry.acr.id
}

resource "aws_s3_bucket" "dr_backups" { bucket = "${var.prefix}-dr-backups" }
resource "aws_s3_bucket_versioning" "v" { bucket = aws_s3_bucket.dr_backups.id versioning_configuration { status = "Enabled" } }
