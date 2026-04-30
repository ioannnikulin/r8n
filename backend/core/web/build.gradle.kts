plugins {
    id("convention-language")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "common web components for backend (exception handlers, etc)"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jakarta.validation)
}
