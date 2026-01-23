# Savedstate

Write pluggable components that save the UI state when a process dies, and restore it when the process restarts. 

[[User Guide](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/savedstate/package-summary)]

## Declaring dependencies

To add a dependency on Savedstate, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Groovy
```
dependencies {
    // Java language implementation
    implementation "androidx.savedstate:savedstate:1.4.0"

    // Kotlin
    implementation "androidx.savedstate:savedstate-ktx:1.4.0"
}
```

### Kotlin
```
dependencies {
    // Java language implementation
    implementation("androidx.savedstate:savedstate:1.4.0")

    // Kotlin
    implementation("androidx.savedstate:savedstate-ktx:1.4.0")
}
```

For more information about dependencies, see Add build dependencies.

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:878252)
