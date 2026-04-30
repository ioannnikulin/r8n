plugins {
    id("convention-api")
}

group = "com.r8n.backend.export"
version = "0.0.1-SNAPSHOT"
description = "export service API"

dependencies {
    api(project(":messaging-api"))
    api(project(":opinions-api"))
    api(project(":opinions-api-integration"))
    api(project(":users-api"))
    api(project(":users-api-integration"))
    api(project(":mock-api"))
}
