plugins {
    id("klibs.spring")
    id("klibs.mock")
}

dependencies {
    implementation(libs.kohsuke.githubApi)
    implementation(libs.okhttp)
    implementation(libs.caffeine)
}
