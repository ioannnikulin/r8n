package com.r8n.backend.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import kotlin.apply

@Suppress("unused") // used through reflection in :build-logic:build.gradle.kts
class NonreactiveBackendServiceConventionPlugin : Plugin<Project> {
    // gateway is reactive, so whenever we need to add a non-reactive (blocking MVC) dependency for all non-gateway services, it goes here.
    override fun apply(project: Project) {
        with(project) {
            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            pluginManager.apply("convention-backend-service")

            dependencies.apply {
                add("implementation", project(":core:security"))
                add("implementation", libs.findLibrary("spring-boot-starter-security").get())
                add("implementation", libs.findLibrary("spring-boot-starter-web").get())
                add("implementation", libs.findLibrary("spring-swagger").get())
                add("testImplementation", libs.findLibrary("spring-boot-starter-webmvc-test").get())
            }
        }
    }
}