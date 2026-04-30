plugins {
    id("convention-integration")
}

group = "com.r8n.backend.messaging"
version = "0.0.1-SNAPSHOT"
description = "messaging service integration package as entry point for other services"

dependencies {
    implementation(project(":messaging-api"))
    implementation(project(":messaging-api-integration"))
}
