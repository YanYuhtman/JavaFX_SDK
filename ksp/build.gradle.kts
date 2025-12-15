plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("kapt")
    id("maven-publish")
}

group = "com.ileveli"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":annotations"))

    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-1.0.31")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "ksp"
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()  // publish to ~/.m2/repository
    }
}