# Compose

Define your UI programmatically with composable functions that describe its shape and data dependencies. 

[[User Guide](https://developer.android.com/jetpack/compose/tutorial)] 

[[Code Sample](https://github.com/android/compose-samples)]

## Structure

Compose is combination of 7 Maven Group Ids within `androidx`. Each Group contains a targeted subset of functionality, each with its own set of release notes.
This table explains the groups and links to each set of release notes.

| Group | Description |
| --- | --- |
| [androidx.compose.animation](https://developer.android.com/jetpack/androidx/releases/compose-animation) | Build animations in their Jetpack Compose applications to enrich the user experience. |
| [androidx.compose.compiler](https://developer.android.com/jetpack/androidx/releases/compose-compiler) | Transform @Composable functions and enable optimizations with a Kotlin compiler plugin. |
| [androidx.compose.foundation](https://developer.android.com/jetpack/androidx/releases/compose-foundation) | Write Jetpack Compose applications with ready to use building blocks and extend foundation to build your own design system pieces. |
| [androidx.compose.material](https://developer.android.com/jetpack/androidx/releases/compose-material) | Build Jetpack Compose UIs with ready to use Material Design Components. This is the higher level entry point of Compose, designed to provide components that match those described at www.material.io. |
| [androidx.compose.material3](https://developer.android.com/jetpack/androidx/releases/compose-material3) | Build Jetpack Compose UIs with Material Design 3 Components, the next evolution of Material Design. Material 3 includes updated theming and components and Material You personalization features like dynamic color, and is designed to be cohesive with the new Android 12 visual style and system UI. |
| [androidx.compose.runtime](https://developer.android.com/jetpack/androidx/releases/compose-runtime) | Fundamental building blocks of Compose's programming model and state management, and core runtime for the Compose Compiler Plugin to target. |
| [androidx.compose.ui](https://developer.android.com/jetpack/androidx/releases/compose-ui) | Fundamental components of compose UI needed to interact with the device, including layout, drawing, and input. |

## Declaring dependencies

To add a dependency on Compose, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Groovy
```groovy
android {
    buildFeatures {
        compose true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

### Kotlin
```kotlin
android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

For more information about dependencies, see [Add build dependencies](https://developer.android.com/studio/build/dependencies).

## BOMs
For the latest BOM releases, visit [Compose BOM Mapping Page](https://developer.android.com/jetpack/compose/bom/bom-mapping).

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:610764)
