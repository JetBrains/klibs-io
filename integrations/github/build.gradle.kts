plugins {
    id("klibs.spring")
    id("klibs.mock")
}

dependencies {
    implementation(libs.kohsuke.githubApi)
    implementation(libs.bundles.jjwt)
    implementation(libs.okhttp)
    implementation(libs.caffeine)
}
