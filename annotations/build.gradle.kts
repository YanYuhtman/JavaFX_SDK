plugins {
    kotlin("jvm") version "2.1.20"
}

group = "com.ileveli.annotations"
version = "1.0.0"

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