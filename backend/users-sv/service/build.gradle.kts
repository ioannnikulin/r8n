plugins {
    id("convention-nonreactive-backend-service")
    id("convention-database-consumer")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "provides information about users"

dependencies {
    implementation(project(":users-api"))
    implementation(project(":users-api-integration"))
    implementation(project(":mock-api"))
    implementation(project(":mock-api-integration"))
    implementation(project(":mock-client"))
    implementation(project(":mock-sv"))
}