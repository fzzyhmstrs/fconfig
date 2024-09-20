/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.Options
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import java.net.URI

plugins {
    id("dev.architectury.loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.modrinth.minotaur") version "2.+"
    id("com.matthewprenger.cursegradle") version "1.4.0"
    `maven-publish`
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val log: File = file("changelog.md")
val minecraftVersion: String by project
val modVersion: String by project
version = "$modVersion+$minecraftVersion+neoforge"
val mavenGroup: String by project
group = mavenGroup
println("## Changelog for FzzyConfig $version \n\n" + log.readText())

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = URI("https://maven.terraformersmc.com/releases/")
        content {
            includeGroup ("com.terraformersmc")
        }
    }
    maven {
        url = URI("https://maven.neoforged.net/releases")
    }
    maven {
        url = URI("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {

    minecraft("com.mojang:minecraft:$minecraftVersion")
    val yarnMappings: String by project
    val yarnMappingsPatchVersion: String by project
    mappings( loom.layered {
        mappings("net.fabricmc:yarn:$yarnMappings:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:$yarnMappingsPatchVersion")
    })
    val loaderVersion: String by project
    neoForge("net.neoforged:neoforge:$loaderVersion")


    val kotlinForForgeVersion: String by project
    modImplementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion")

    val tomlktVersion: String by project
    implementation("net.peanuuutz.tomlkt:tomlkt-jvm:$tomlktVersion")
    include("net.peanuuutz.tomlkt:tomlkt-jvm:$tomlktVersion")

    val janksonVersion: String by project
    implementation("blue.endless:jankson:$janksonVersion")
    include("blue.endless:jankson:$janksonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

tasks {
    val javaVersion = JavaVersion.VERSION_21
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
        //java.sourceCompatibility = javaVersion
        //targetCompatibility = javaVersion.toString()
    }
    jar {
        exclude("me/fzzyhmstrs/fzzy_config/examples/**")
        from("LICENSE") { rename { "${base.archivesName.get()}_${it}" } }
    }
    jar {
        from( "credits.txt") { rename { "${base.archivesName.get()}_${it}" } }
    }
    processResources {
        val loaderVersion: String by project
        val kotlinForForgeVersion: String by project
        inputs.property("version", project.version)
        inputs.property("id", base.archivesName.get())
        inputs.property("loaderVersion", loaderVersion)
        inputs.property("kotlinForForgeVersion", kotlinForForgeVersion)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mutableMapOf(
                "version" to project.version,
                "id" to base.archivesName.get(),
                "loaderVersion" to loaderVersion,
                "kotlinForForgeVersion" to kotlinForForgeVersion
            )) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    modrinth.get().group = "upload"
    modrinthSyncBody.get().group = "upload"
}


if (System.getenv("MODRINTH_TOKEN") != null) {
    modrinth {
        val releaseType: String by project
        val mcVersions: String by project
        val uploadDebugMode: String by project

        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("fzzy-config")
        versionNumber.set("${project.version}")
        versionName.set("${base.archivesName.get()}-${project.version}")
        versionType.set(releaseType)
        uploadFile.set(tasks.remapJar.get())
        additionalFiles.add(tasks.remapSourcesJar.get().archiveFile)
        gameVersions.addAll(mcVersions.split(","))
        loaders.addAll("neoforge")
        detectLoaders.set(false)
        changelog.set(log.readText())
        dependencies {
            required.project("kotlin-for-forge")
        }
        debugMode.set(uploadDebugMode.toBooleanLenient() ?: true)
    }
}

if (System.getenv("CURSEFORGE_TOKEN") != null) {
    curseforge {
        val releaseType: String by project
        val mcVersions: String by project
        val uploadDebugMode: String by project

        apiKey = System.getenv("CURSEFORGE_TOKEN")
        project(closureOf<CurseProject> {
            id = "1005914"
            changelog = log
            changelogType = "markdown"
            this.releaseType = releaseType
            for (ver in mcVersions.split(",")) {
                addGameVersion(ver)
            }
            addGameVersion("NeoForge")
            mainArtifact(tasks.remapJar.get().archiveFile.get(), closureOf<CurseArtifact> {
                displayName = "${base.archivesName.get()}-${project.version}"
                relations(closureOf<CurseRelation> {
                    this.requiredDependency("kotlin-for-forge")
                })
            })
            addArtifact(tasks.remapSourcesJar.get().archiveFile, closureOf<CurseArtifact> {
                changelogType = "markdown"
                changelog = "Source files for ${base.archivesName.get()}-${project.version}"
            })
            relations(closureOf<CurseRelation> {
                this.requiredDependency("kotlin-for-forge")
            })
        })
        options(closureOf<Options> {
            javaIntegration = false
            forgeGradleIntegration = false
            javaVersionAutoDetect = false
            debug = uploadDebugMode.toBooleanLenient() ?: true
        })
    }
}

tasks.register("uploadAll") {
    group = "upload"
    dependsOn(tasks.modrinth.get())
    dependsOn(tasks.curseforge.get())
    dependsOn(tasks.publish.get())
}

publishing {
    publications {
        create<MavenPublication>("fzzyConfig") {
            from(components["java"])

            pom {
                name.set("Fzzy Config")
                description.set("Configuration engine with automatic GUI generation, client-server syncing, powerful validation and error handling, and much more.")
                inceptionYear.set("2024")
                licenses {
                    license {
                        name.set("TDL-M")
                        url.set("https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified")
                        distribution.set("repo")
                        comments.set(" Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M). See license url for full license details.")
                    }
                }
                scm {
                    url.set("https://github.com/fzzyhmstrs/fconfig")
                }
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/fzzyhmstrs/fconfig/issues")
                }
                developers {
                    developer {
                        name.set("Fzzyhmstrs")
                        url.set("https://github.com/fzzyhmstrs")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "FzzyMaven"
            url = uri("https://maven.fzzyhmstrs.me")
            credentials {
                username = System.getProperty("fzzyMavenUsername")
                password = System.getProperty("fzzyMavenPassword")
            }
        }
    }
}