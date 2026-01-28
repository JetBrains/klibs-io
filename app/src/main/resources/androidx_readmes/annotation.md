# Annotation

Expose metadata that helps tools and other developers understand your app's code. 

[[API Reference](https://developer.android.com/reference/androidx/annotation/package-summary)]

## Declaring dependencies

To add a dependency on Annotation, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Kotlin
```kotlin
dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    // To use the Java-compatible @androidx.annotation.OptIn API annotation
    implementation("androidx.annotation:annotation-experimental:1.5.1")
}
```

<details>
<summary><b>Groovy</b></summary>

```groovy
dependencies {
    implementation "androidx.annotation:annotation:1.9.1"
    // To use the Java-compatible @androidx.annotation.OptIn API annotation
    implementation "androidx.annotation:annotation-experimental:1.5.1"
}
```

</details>

For more information about dependencies, see Add build dependencies.

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:459778)
