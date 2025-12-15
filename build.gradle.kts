plugins {
    kotlin("jvm") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.31" apply false
    id("maven-publish")
}

allprojects {
    repositories {
        mavenCentral()
    }

}
gradle.projectsEvaluated {
    tasks.named("publish").configure {
        dependsOn(
            subprojects.map { it.tasks.named("publish") }
        )
    }
}
gradle.projectsEvaluated {
    tasks.named("publishToMavenLocal").configure {
        dependsOn(
            subprojects.map { it.tasks.named("publishToMavenLocal") }
        )
    }
}