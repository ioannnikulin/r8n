plugins {
    id("convention-integration")
}

group = "com.r8n.backend.opinions"
version = "0.0.1-SNAPSHOT"
description = "opinions service integration package as entry point for other services"

dependencies {
    implementation(project(":opinions-api"))
    implementation(project(":opinions-api-integration"))
}
