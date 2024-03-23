plugins {
    id("fabric-loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization") version "1.9.22"
    id("com.modrinth.minotaur") version "2.+"
}
base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}
val log: File = file("changelog.md")
val modVersion: String by project
version = modVersion
val mavenGroup: String by project
group = mavenGroup
println("## Changelog for FzzyConfig $modVersion \n\n" + log.readText())
repositories {
}
dependencies {
    val minecraftVersion: String by project
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
    include("net.peanuuutz.tomlkt:tomlkt:$tomlktVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
sourceSets{
    main{
        kotlin{
            exclude ( "me/fzzyhmstrs/fzzy_config/examples/**" )
        }
    }
}
tasks {
    val javaVersion = JavaVersion.VERSION_17
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
        from("LICENSE") { rename { "${it}_${base.archivesName}" } }
    }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("fzzy-config")
    versionNumber.set(modVersion)
    versionName.set("${base.archivesName.get()}-$modVersion")
    versionType.set("beta")
    uploadFile.set(tasks.remapJar.get())
    gameVersions.addAll("1.19.3")
    loaders.addAll("fabric","quilt")
    detectLoaders.set(false)
    changelog.set("## Changelog for Fzzy Config $modVersion \n\n" + log.readText())
    dependencies{
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        optional.project("trinkets")
    }
    debugMode.set(true)
}