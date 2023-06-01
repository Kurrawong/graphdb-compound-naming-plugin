val graphdbSdkVersion: String by project
val graphdbVersion: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "com.edmondchuc.graphdb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "GraphDB Releases"
        url = uri("https://maven.ontotext.com/repository/owlim-releases")
    }
}

dependencies {
    testImplementation(kotlin("test"))
//    implementation("com.ontotext.graphdb:graphdb-sdk:$graphdbSdkVersion")
    implementation("com.ontotext.graphdb:graphdb-tests-base:$graphdbVersion")
    implementation("com.ontotext.graphdb:graphdb-runtime:$graphdbVersion")
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