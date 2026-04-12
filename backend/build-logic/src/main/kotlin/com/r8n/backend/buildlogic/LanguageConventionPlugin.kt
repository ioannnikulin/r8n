package com.r8n.backend.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

@Suppress("unused") // used through reflection in :build-logic:build.gradle.kts
class LanguageConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {

            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            pluginManager.apply(libs.findPlugin("ktlint").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("kotlin-jvm").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("kotlin-spring").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("spring-dependency-management").get().get().pluginId)

            extensions.configure(JavaPluginExtension::class.java) {
                toolchain.languageVersion.set(JavaLanguageVersion.of(21))
                withSourcesJar()
            }

            extensions.configure(KotlinJvmProjectExtension::class.java) {
                jvmToolchain(21)
            }

            tasks.withType(KotlinCompile::class.java).configureEach {
                compilerOptions.freeCompilerArgs.addAll(
                    "-Xjsr305=strict",
                    "-Xannotation-default-target=param-property",
                )
            }

            extensions.configure(KtlintExtension::class.java) {
                filter {
                    exclude { element -> element.file.path.contains("generated/") }
                }
            }

            tasks.withType(Test::class.java).configureEach {
                useJUnitPlatform()
                this.failOnNoDiscoveredTests.set(false)
            }

            dependencies.apply {
                add("implementation", enforcedPlatform(project(":platform")))

                add("implementation", libs.findLibrary("jackson-module-kotlin").get())
                add("implementation", libs.findLibrary("kotlin-reflect").get())
            }
        }
    }
}