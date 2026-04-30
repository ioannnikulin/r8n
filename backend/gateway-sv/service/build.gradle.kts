import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("convention-backend-service")
}

tasks.named<BootJar>("bootJar") {
    mainClass.set("com.r8n.backend.gateway.GatewayApplicationKt")
}

dependencies {
    implementation(platform(project(":platform")))
    implementation(project(":core:security-reactive"))
    implementation(project(":users-api-integration"))
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.swagger.gateway)
}
