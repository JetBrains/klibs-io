plugins {
    id("klibs.base")
    alias(libs.plugins.jib) apply false
}

// Use this task to update kotlin version
tasks.updateDaemonJvm {
    languageVersion = JavaLanguageVersion.of(21)
}