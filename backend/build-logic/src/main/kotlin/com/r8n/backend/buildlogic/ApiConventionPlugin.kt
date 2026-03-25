package com.r8n.backend.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import kotlin.apply

@Suppress("unused") // used through reflection in :build-logic:build.gradle.kts
class ApiConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            pluginManager.apply("convention-language")

            dependencies.apply {
                add("implementation", project(":core:api"))
                add("implementation", project(":core:utils"))

                add("compileOnly", libs.findLibrary("spring-web").get())
            }
        }
    }
}
