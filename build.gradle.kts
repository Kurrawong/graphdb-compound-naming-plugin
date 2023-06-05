val projectVersion: String by project
val graphdbSdkVersion: String by project
val graphdbVersion: String by project
val eclipseCollectionsVersion: String by project
val rdf4jVersion: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "ai.kurrawong.graphdb"
version = projectVersion

repositories {
    mavenCentral()
    maven {
        name = "GraphDB Releases"
        url = uri("https://maven.ontotext.com/repository/owlim-releases")
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.20-RC")
    implementation("com.ontotext.graphdb:graphdb-tests-base:$graphdbVersion")
    implementation("com.ontotext.graphdb:graphdb-runtime:$graphdbVersion")
    implementation("org.eclipse.collections:eclipse-collections:$eclipseCollectionsVersion")
    implementation("org.eclipse.rdf4j:rdf4j-model:$rdf4jVersion")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}