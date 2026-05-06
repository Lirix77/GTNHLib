import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import org.gradle.kotlin.dsl.named

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// workaround an OOM on GH actions during the checkStyleMain task
tasks.withType<Checkstyle>().configureEach {
    maxHeapSize = "1g"
}

tasks.test.configure {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val functionalTest by sourceSets.creating {
    java {
        srcDir("src/functionalTest/java")
        compileClasspath += sourceSets.patchedMc.get().output + sourceSets.main.get().output
    }
}

configurations {
    // Keep all dependencies from the main mod in the functional test mod
    named(functionalTest.compileClasspathConfigurationName).configure {extendsFrom(configurations.compileClasspath.get())}
    named(functionalTest.runtimeClasspathConfigurationName).configure {extendsFrom(configurations.runtimeClasspath.get())}
    named(functionalTest.annotationProcessorConfigurationName).configure {extendsFrom(configurations.annotationProcessor.get())}
}

tasks.register<Jar>(functionalTest.jarTaskName) {
    archiveClassifier.set("functionalTests")
    // we don't care about the version number here, keep it stable to avoid polluting the tmp directory
    archiveVersion.set("1.0")
    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
}

tasks.assemble.configure {
    dependsOn(functionalTest.jarTaskName)
}

// Run tests in the default runServer/runClient configurations
tasks.named<RunMinecraftTask>("runServer").configure {
    dependsOn(functionalTest.jarTaskName)
    classpath(configurations.named(functionalTest.runtimeClasspathConfigurationName), tasks.named(functionalTest.jarTaskName))
}

tasks.named<RunMinecraftTask>("runClient").configure {
    dependsOn(functionalTest.jarTaskName)
    classpath(configurations.named(functionalTest.runtimeClasspathConfigurationName), tasks.named(functionalTest.jarTaskName))
}
