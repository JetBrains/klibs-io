plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.mock")
    id("klibs.spring-scheduling")
    id("klibs.spring-cloud")
    id("klibs.mapping")
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptTask>().configureEach {
    if (name.contains("Test")) {
        enabled = false
    }
}

tasks.bootJar {
    enabled = true
}

springBoot {
    mainClass.set("io.klibs.app.ApplicationKt")
}

tasks.bootRun {
    workingDir = rootProject.projectDir
}

dependencies {
    implementation(projects.core.`package`)
    implementation(projects.core.project)
    implementation(projects.core.readme)
    implementation(projects.core.scmOwner)
    implementation(projects.core.scmRepository)
    implementation(projects.core.search)
    implementation(projects.core.storage)

    implementation(projects.integrations.ai)
    implementation(projects.integrations.maven)
    implementation(projects.integrations.github)
    implementation(projects.integrations.mcp)

    implementation(libs.bundles.maven.indexer)

    testImplementation(libs.okhttp)
    testImplementation(libs.kohsuke.githubApi)
}
