plugins {
    id("convention-integration")
}

group = "com.r8n.backend.users"
version = "0.0.1-SNAPSHOT"
description = "users service integration package as entry point for other services"

dependencies {
    implementation(project(":users-api-integration"))
}
