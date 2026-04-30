plugins {
    id("convention-nonreactive-backend-service")
    id("convention-database-consumer")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "support messaging service"

dependencies {
    implementation(project(":messaging-api"))
    implementation(project(":messaging-api-integration"))
}
