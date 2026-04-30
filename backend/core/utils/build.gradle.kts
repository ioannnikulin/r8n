plugins {
    id("convention-language")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "misc stuff for backend"

dependencies {
    implementation(project(":core:api"))
    implementation(libs.spring.data.commons)
    implementation(libs.spring.boot.starter.test)
}
