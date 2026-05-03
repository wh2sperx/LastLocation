plugins {
    `java-library`
    kotlin("jvm") version "2.3.10"
}

group = "dev.arclyx0"
version = "v1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.onarandombox.com/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.0.0-SNAPSHOT")
    implementation("com.h2database:h2:2.4.240")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation(kotlin("stdlib"))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("LastLocation")
    archiveVersion.set(project.version.toString())
}
