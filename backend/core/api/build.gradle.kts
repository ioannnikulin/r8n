plugins {
    id("convention-language")
}

group = "com.r8n.backend"
version = "0.0.1-SNAPSHOT"
description = "common api components for backend"

dependencies {
    compileOnly(libs.spring.boot.starter.validation)
    implementation(libs.jakarta.validation)
}
