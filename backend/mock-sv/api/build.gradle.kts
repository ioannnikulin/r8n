plugins {
    id("convention-api")
}

group = "com.r8n.backend.mock"
version = "0.0.1-SNAPSHOT"
description = "mock service API"

dependencies {
    implementation(project(":opinions-api"))
}
