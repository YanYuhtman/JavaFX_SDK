plugins {
    kotlin("jvm") version "2.1.20"
    id("maven-publish")
}

group = "com.ileveli"
version = "1.0.2"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain( 17 )
}

dependencies {
    implementation(kotlin("stdlib"))
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
            artifactId = "annotations"
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()  // publish to ~/.m2/repository
    }
}