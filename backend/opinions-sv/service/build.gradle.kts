plugins {
    id("convention-nonreactive-backend-service")
    id("convention-database-consumer")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "manages opinions, lists, and access to them"

dependencies {
    implementation(project(":opinions-api"))
    implementation(project(":opinions-api-integration"))
    implementation(project(":users-api-integration"))
    implementation(project(":users-client"))
}
