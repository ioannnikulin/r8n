plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(platform(libs.spring.boot.dependencies))
        api(platform(libs.spring.cloud.bom))

        api(libs.guava)
        api(libs.kotlin.reflect)
        api(libs.spring.boot.dependencies)
        api(libs.spring.boot.starter)
        api(libs.spring.boot.starter.security)
        api(libs.spring.boot.starter.test)
        api(libs.spring.boot.starter.validation)
        api(libs.spring.data.commons)
        api(libs.testcontainers.junit)
        api(libs.testcontainers.postgresql)
    }
}