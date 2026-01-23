# Collection

Reduce the memory impact of existing and new collections that are small. 

[[API Reference](https://developer.android.com/reference/kotlin/androidx/collection/package-summary)]

## Declaring dependencies

To add a dependency on Collection, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Groovy
```
dependencies {
    def collection_version = "1.5.0"
    implementation "androidx.collection:collection:$collection_version"
}
```

### Kotlin
```
dependencies {
    val collection_version = "1.5.0"
    implementation("androidx.collection:collection:$collection_version")
}
```

For more information about dependencies, see Add Build Dependencies.

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:460756)
