plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.mock")
    id("klibs.spring-scheduling")
    id("klibs.spring-cloud")
}

tasks.bootJar {
    enabled = true
}

springBoot {
    mainClass.set("io.klibs.app.ApplicationKt")
}

dependencies {
    implementation(projects.core.`package`)
    implementation(projects.core.project)
    implementation(projects.core.scmOwner)
    implementation(projects.core.scmRepository)
    implementation(projects.core.search)
    implementation(projects.core.storage)

    implementation(projects.integrations.ai)
    implementation(projects.integrations.maven)
    implementation(projects.integrations.github)

    testImplementation(libs.okhttp)
    testImplementation(libs.kohsuke.githubApi)
}
