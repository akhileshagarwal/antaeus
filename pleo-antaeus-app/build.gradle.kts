plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

kotlinProject()

dataLibs()

application {
    mainClassName = "io.pleo.antaeus.app.AntaeusApp"
}

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation(project(":pleo-antaeus-rest"))
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))
}

// Shadow task depends on Jar task, so these configs are reflected for Shadow as well
tasks.jar {
    manifest.attributes["Main-Class"] = "io.pleo.antaeus.app.AntaeusApp"
}
