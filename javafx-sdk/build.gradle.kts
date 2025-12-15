val mainModuleName:String by project
val mainClassName:String by project
val mainLauncherClassName:String by project

val myGroupId:String by project
val myArtifactId:String by project
val myAppVersion:String by project

val myAppName:String by project
val myVendor:String by project
val myCopyright:String by project
val myAppDescription:String by project


plugins {
    val kotlinVersion = "2.1.20"

    java
    application
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
    id("maven-publish")
//    kotlin("kapt")
    id("com.google.devtools.ksp") version "2.1.20-1.0.31"
}

group = myGroupId
version = myAppVersion

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set(mainModuleName)
    mainClass.set(mainClassName)
}
kotlin {
    jvmToolchain( 17 )
}

plugins.withType<JavaPlugin>().configureEach {
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
        modularity.inferModulePath = true
    }
}
javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}


publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["java"])
            groupId = myGroupId
            artifactId = myArtifactId
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()  // publish to ~/.m2/repository
    }
}
ksp {
    arg("genClassName", "Strings")
    arg("debug", "true")
    arg("logLevel", "verbose")
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":annotations"))
    ksp(project(":processor"))

    //Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.20")

    //Tests
    testImplementation(kotlin("test"))

    //Coroutines
    var coroutinesVersion = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:${coroutinesVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${coroutinesVersion}")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")


}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}
