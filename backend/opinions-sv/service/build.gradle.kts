plugins {
    id("convention-nonreactive-backend-service")
    id("convention-database-consumer")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "provides individual opinions"

dependencies {
    implementation(project(":opinions-api"))
    implementation(project(":users-api-integration"))
    implementation(project(":users-client"))
    implementation(project(":mock-sv"))
    testImplementation(project(":mock-api"))
}