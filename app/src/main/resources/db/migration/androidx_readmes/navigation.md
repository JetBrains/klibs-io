# Navigation

Navigation is a framework for navigating between &#39;destinations&#39; within an Android application that provides a consistent API whether destinations are implemented as Fragments, Activities, or other components. 

[[User Guide](https://developer.android.com/guide/navigation)] 

[[Code Sample](https://github.com/android/architecture-components-samples)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/navigation/package-summary)]

## Declaring dependencies

To add a dependency on Navigation, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Groovy
```
plugins {
  // Kotlin serialization plugin for type safe routes and navigation arguments
  id 'org.jetbrains.kotlin.plugin.serialization' version '2.0.21'
}
  
dependencies {
  def nav_version = "2.9.6"

  // Jetpack Compose Integration
  implementation "androidx.navigation:navigation-compose:$nav_version"

  // Views/Fragments Integration
  implementation "androidx.navigation:navigation-fragment:$nav_version"
  implementation "androidx.navigation:navigation-ui:$nav_version"

  // Feature module support for Fragments
  implementation "androidx.navigation:navigation-dynamic-features-fragment:$nav_version"

  // Testing Navigation
  androidTestImplementation "androidx.navigation:navigation-testing:$nav_version"

  // JSON serialization library, works with the Kotlin serialization plugin.
  implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"
}
```

### Kotlin
```
plugins {
  // Kotlin serialization plugin for type safe routes and navigation arguments
  kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
  val nav_version = "2.9.6"

  // Jetpack Compose integration
  implementation("androidx.navigation:navigation-compose:$nav_version")

  // Views/Fragments integration
  implementation("androidx.navigation:navigation-fragment:$nav_version")
  implementation("androidx.navigation:navigation-ui:$nav_version")

  // Feature module support for Fragments
  implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")

  // Testing Navigation
  androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

  // JSON serialization library, works with the Kotlin serialization plugin
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

## Safe Args

To add Safe Args to your project, include the following `classpath` in your top level `build.gradle` file:

### Groovy
```
buildscript {
  repositories {
    google()
  }
  dependencies {
    def nav_version = "2.9.6"
    classpath "androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version"
  }
}
```

### Kotlin
```
buildscript {
  repositories {
    google()
  }
  dependencies {
    val nav_version = "2.9.6"
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
  }
}
```

You must also apply one of two available plugins.

To generate Java language code suitable for Java or mixed Java and Kotlin modules, add this line to **your app or module's** `build.gradle` file:

### Groovy
```
plugins {
  id 'androidx.navigation.safeargs'
}
```

### Kotlin
```
plugins {
  id("androidx.navigation.safeargs")
}
```

Alternatively, to generate Kotlin code suitable for Kotlin-only modules, add:

### Groovy
```
plugins {
  id 'androidx.navigation.safeargs.kotlin'
}
```

### Kotlin
```
plugins {
  id("androidx.navigation.safeargs.kotlin")
}
```

You must have `android.useAndroidX=true` in your [`gradle.properties` file](https://developer.android.com/studio/build#properties-files) as per [Migrating to AndroidX](https://developer.android.com/jetpack/androidx/migrate).

For information on using Kotlin extensions, see the [ktx documentation](https://developer.android.com/kotlin/ktx).

For more information about dependencies, see [Add Build Dependencies](https://developer.android.com/studio/build/dependencies).

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:409828)
