plugins {
    id("convention-language")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "Common security logic for all services"

dependencies {
    implementation(libs.spring.boot.starter.oauth)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.web)
}
