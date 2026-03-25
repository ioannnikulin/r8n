package com.r8n.backend.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import kotlin.apply

@Suppress("unused") // used through reflection in :build-logic:build.gradle.kts
class BackendServiceConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            pluginManager.apply("convention-language")
            pluginManager.apply(libs.findPlugin("kotlin-allopen").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("spring-boot").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("spring-dependency-management").get().get().pluginId)

            dependencies.apply {
                add("implementation", project(":core:api"))
                add("implementation", project(":core:utils"))

                add("implementation", libs.findLibrary("jackson-jsr310").get())
                add("implementation", libs.findLibrary("jackson-module-kotlin").get())
                add("implementation", libs.findLibrary("spring-boot-starter").get())
                add("implementation", libs.findLibrary("spring-data-commons").get())

                add("testImplementation", libs.findLibrary("mockito").get())
                add("testImplementation", libs.findLibrary("spring-boot-starter-test").get())
                add("testImplementation", libs.findLibrary("spring-boot-test-autoconfigure").get())
                add("testImplementation", libs.findLibrary("spring-security-test").get())
            }
        }
    }
}