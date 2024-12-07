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
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import java.net.URI

plugins {
    id("fabric-loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.modrinth.minotaur") version "2.+"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.matthewprenger.cursegradle") version "1.4.0"
    `maven-publish`
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.20")
        classpath("org.jetbrains.dokka:versioning-plugin:1.9.20")
    }
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val log: File = file("changelog.md")
val minecraftVersion: String by project
val modVersion: String by project
version = "$modVersion+$minecraftVersion"
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
}

sourceSets {
    main {
        kotlin {
            val includeExamples: String by project
            if (includeExamples.toBooleanLenient() != true) {
                exclude("me/fzzyhmstrs/fzzy_config/examples/**")
            }
        }
    }
    create("testmod"){
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}

val testmodImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

idea {
    module {
        testSources.from(sourceSets["testmod"].java.srcDirs)
        testSources.from(sourceSets["testmod"].kotlin.srcDirs)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    val yarnMappings: String by project
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    val loaderVersion: String by project
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    val fabricVersion: String by project
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    val tomlktVersion: String by project
    implementation("net.peanuuutz.tomlkt:tomlkt:$tomlktVersion")
    include("net.peanuuutz.tomlkt:tomlkt-jvm:$tomlktVersion")

    val janksonVersion: String by project
    implementation("blue.endless:jankson:$janksonVersion")
    include("blue.endless:jankson:$janksonVersion")

    val fabricPermsVersion: String by project
    modImplementation("me.lucko:fabric-permissions-api:$fabricPermsVersion")
    include("me.lucko:fabric-permissions-api:$fabricPermsVersion")

    val modmenuVersion: String by project
    modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion") {
        isTransitive = false
    }
    modLocalRuntime("com.terraformersmc:modmenu:$modmenuVersion") {
        isTransitive = false
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testmodImplementation(sourceSets.main.get().output)

    dokkaPlugin("me.fzzyhmstrs:internal-skip-plugin:1.0-SNAPSHOT")
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.20")
}

loom {
    runs {
        create("testmodClient"){
            client()
            name = "Testmod Client"
            source(sourceSets["testmod"])
        }
    }
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
        val fabricKotlinVersion: String by project
        inputs.property("version", project.version)
        inputs.property("id", base.archivesName.get())
        inputs.property("loaderVersion", loaderVersion)
        inputs.property("fabricKotlinVersion", fabricKotlinVersion)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to project.version,
                "id" to base.archivesName.get(),
                "loaderVersion" to loaderVersion,
                "fabricKotlinVersion" to fabricKotlinVersion
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

val testmodJar =  tasks.register("testmodJar", Jar::class) {
    from(sourceSets["testmod"].output)
    destinationDirectory =  File(project.layout.buildDirectory.get().asFile, "testmod")
    archiveClassifier = "testmod"
}

val remapTestmodJar =  tasks.register("remapTestmodJar", RemapJarTask::class){
    dependsOn(testmodJar.get())
    input.set(testmodJar.get().archiveFile)
    archiveClassifier = "testmod"
    addNestedDependencies = false
    //destinationDirectory =  File(project.layout.buildDirectory.get().asFile, "testmod")
}

tasks.build{
    dependsOn(remapTestmodJar.get())
}

tasks.withType<DokkaTask>().configureEach {

    inputs.dir(file("dokka"))

    val docVersionsDir = projectDir.resolve("build/dokka/version")
    // The version for which you are currently generating docs
    val currentVersion = project.version.toString()

    // Set the output to a folder with all other versions
    // as you'll need the current version for future builds
    val currentDocsDir = docVersionsDir.resolve(currentVersion)
    outputDirectory.set(currentDocsDir)
    dokkaSourceSets.configureEach {
        perPackageOption {
            matchingRegex.set("me.fzzyhmstrs.fzzy_config.examples|me.fzzyhmstrs.fzzy_config.impl|me.fzzyhmstrs.fzzy_config.test|me.fzzyhmstrs.fzzy_config.updates|me.fzzyhmstrs.fzzy_config")
            suppress.set(true)
        }
        includes.from(project.files(), "dokka/module.md")
    }
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        moduleName = "Fzzy Config"
        customAssets = listOf(
            file("dokka/assets/fc_banner.png"),
            file("dokka/assets/discord_banner.png"),
            file("dokka/assets/discord_small_banner.png"),
            file("dokka/assets/wiki_banner.png"),
            file("dokka/assets/wiki_small_banner.png"),
            file("dokka/assets/docs_banner.png"),
            file("dokka/assets/docs_small_banner.png"),
            file("dokka/assets/cf_banner.png"),
            file("dokka/assets/modrinth_banner.png"),
            file("src/main/resources/icon.png"))
        customStyleSheets = listOf(file("dokka/style.css"),file("dokka/logo-styles.css"))
        templatesDir = file("dokka")
        footerMessage = "(c) 2024 fzzyhmstrs"
    }

    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        olderVersionsDir = docVersionsDir
        version = currentVersion
    }

    doLast {
        // This folder contains the latest documentation with all
        // previous versions included, so it's ready to be published.
        // Make sure it's copied and not moved - you'll still need this
        // version for future builds
        currentDocsDir.copyRecursively(file("docs"), overwrite = true)

        // Only once current documentation has been safely moved,
        // remove previous versions bundled in it. They will not
        // be needed in future builds, it's just overhead.
        currentDocsDir.resolve("older").deleteRecursively()
    }
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
        loaders.addAll("fabric", "quilt")
        detectLoaders.set(false)
        changelog.set(log.readText())
        dependencies {
            required.project("fabric-api")
            required.project("fabric-language-kotlin")
        }
        debugMode.set(uploadDebugMode.toBooleanLenient() ?: true)
    }
}

if (System.getenv("CURSEFORGE_TOKEN") != null) {
    curseforge {
        val releaseType: String by project
        val mcVersions: String by project
        val mcCurseVersions: String by project
        val uploadDebugMode: String by project

        apiKey = System.getenv("CURSEFORGE_TOKEN")
        project(closureOf<CurseProject> {
            id = "1005914"
            changelog = log
            changelogType = "markdown"
            this.releaseType = releaseType
            val v = if (mcCurseVersions.isEmpty()) mcVersions else mcCurseVersions
            for (ver in v.split(",")) {
                addGameVersion(ver)
            }
            addGameVersion("Fabric")
            addGameVersion("Quilt")
            mainArtifact(tasks.remapJar.get().archiveFile.get(), closureOf<CurseArtifact> {
                displayName = "${base.archivesName.get()}-${project.version}"
                relations(closureOf<CurseRelation>{
                    this.requiredDependency("fabric-api")
                    this.requiredDependency("fabric-language-kotlin")
                })
            })
            addArtifact(tasks.remapSourcesJar.get().archiveFile, closureOf<CurseArtifact> {
                changelogType = "markdown"
                changelog = "Source files for ${base.archivesName.get()}-${project.version}"
            })
            relations(closureOf<CurseRelation>{
                this.requiredDependency("fabric-api")
                this.requiredDependency("fabric-language-kotlin")
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