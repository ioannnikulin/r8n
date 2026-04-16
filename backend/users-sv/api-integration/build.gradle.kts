plugins {
    id("convention-api")
}

group = "com.r8n.backend.users"
version = "0.0.1-SNAPSHOT"

dependencies {
    api(project(":core:api"))
}