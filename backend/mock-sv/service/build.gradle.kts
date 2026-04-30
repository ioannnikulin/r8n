plugins {
    id("convention-nonreactive-backend-service")
}

group = "com.r8n.backend.mock"
version = "0.0.1-SNAPSHOT"
description = "mock service for everything not cut out into dedicated services yet"

dependencies {
    implementation(project(":mock-api"))
    implementation(project(":mock-api-integration"))
    implementation(project(":opinions-api"))
    implementation(project(":opinions-api-integration"))
}
