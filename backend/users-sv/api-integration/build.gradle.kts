plugins {
    id("convention-api")
}

group = "com.r8n.backend.users"
version = "0.0.1-SNAPSHOT"
description = "user information service API for other services' use"

dependencies {
    implementation(project(":users-api"))
}
