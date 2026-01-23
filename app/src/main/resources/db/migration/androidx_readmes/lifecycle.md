# Lifecycle

Lifecycle-aware components perform actions in response to a change in the lifecycle status of another component, such as activities and fragments. These components help you produce better-organized, and often lighter-weight code, that is easier to maintain. 

[[User Guide](https://developer.android.com/topic/libraries/architecture/lifecycle)] 

[[Code Sample](https://github.com/android/architecture-components-samples)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/lifecycle/package-summary)]

## Declaring dependencies

To add a dependency on Lifecycle, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

## Kotlin

### Groovy
```
dependencies {
        def lifecycle_version = "2.10.0"
        def arch_version = "2.2.0"

        // ViewModel
        implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
        // ViewModel utilities for Compose
        implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version"
        // LiveData
        implementation "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"
        // Lifecycles only (without ViewModel or LiveData)
        implementation "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"
        // Lifecycle utilities for Compose
        implementation "androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version"

        // Saved state module for ViewModel
        implementation "androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version"

        // ViewModel integration with Navigation3
        implementation "androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0"

        // Annotation processor
        kapt "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"
        // alternately - if using Java8, use the following instead of lifecycle-compiler
        implementation "androidx.lifecycle:lifecycle-common-java8:$lifecycle_version"

        // optional - helpers for implementing LifecycleOwner in a Service
        implementation "androidx.lifecycle:lifecycle-service:$lifecycle_version"

        // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
        implementation "androidx.lifecycle:lifecycle-process:$lifecycle_version"

        // optional - ReactiveStreams support for LiveData
        implementation "androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycle_version"

        // optional - Test helpers for LiveData
        testImplementation "androidx.arch.core:core-testing:$arch_version"

        // optional - Test helpers for Lifecycle runtime
        testImplementation "androidx.lifecycle:lifecycle-runtime-testing:$lifecycle_version"
    }
```

## Java

### Groovy
```
dependencies {
        def lifecycle_version = "2.10.0"
        def arch_version = "2.2.0"

        // ViewModel
        implementation "androidx.lifecycle:lifecycle-viewmodel:$lifecycle_version"
        // LiveData
        implementation "androidx.lifecycle:lifecycle-livedata:$lifecycle_version"
        // Lifecycles only (without ViewModel or LiveData)
        implementation "androidx.lifecycle:lifecycle-runtime:$lifecycle_version"

        // Saved state module for ViewModel
        implementation "androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version"

        // Annotation processor
        annotationProcessor "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"
        // alternately - if using Java8, use the following instead of lifecycle-compiler
        implementation "androidx.lifecycle:lifecycle-common-java8:$lifecycle_version"

        // optional - helpers for implementing LifecycleOwner in a Service
        implementation "androidx.lifecycle:lifecycle-service:$lifecycle_version"

        // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
        implementation "androidx.lifecycle:lifecycle-process:$lifecycle_version"

        // optional - ReactiveStreams support for LiveData
        implementation "androidx.lifecycle:lifecycle-reactivestreams:$lifecycle_version"

        // optional - Test helpers for LiveData
        testImplementation "androidx.arch.core:core-testing:$arch_version"

        // optional - Test helpers for Lifecycle runtime
        testImplementation "androidx.lifecycle:lifecycle-runtime-testing:$lifecycle_version"
    }
```

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:413132)
