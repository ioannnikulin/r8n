plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.kotlin.gradle)
    implementation(libs.spring.boot.gradle)
    implementation(libs.kotlin.jpa)
}

gradlePlugin {
    plugins {
        create("convention-language") {
            id = "convention-language"
            implementationClass = "com.r8n.backend.buildlogic.LanguageConventionPlugin"
        }
        create("convention-api") {
            id = "convention-api"
            implementationClass = "com.r8n.backend.buildlogic.ApiConventionPlugin"
        }
        create("convention-integration") {
            id = "convention-integration"
            implementationClass = "com.r8n.backend.buildlogic.IntegrationConventionPlugin"
        }
        create("convention-backend-service") {
            id = "convention-backend-service"
            implementationClass = "com.r8n.backend.buildlogic.BackendServiceConventionPlugin"
        }
        create("convention-nonreactive-backend-service") {
            id = "convention-nonreactive-backend-service"
            implementationClass = "com.r8n.backend.buildlogic.NonreactiveBackendServiceConventionPlugin"
        }
        create("convention-database-consumer") {
            id = "convention-database-consumer"
            implementationClass = "com.r8n.backend.buildlogic.DatabaseConsumerConventionPlugin"
        }
    }
}