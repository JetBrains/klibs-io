# Room

The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite. 

[[User Guide](https://developer.android.com/training/data-storage/room)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/room/package-summary)]

## Declaring dependencies

To add a dependency on Room, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Kotlin
```kotlin
dependencies {
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")
    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")
    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")
    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")
    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")
}
```

<details>
<summary><b>Groovy</b></summary>

```groovy
dependencies {
    def room_version = "2.8.4"
    implementation "androidx.room:room-runtime:$room_version"
    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    ksp "androidx.room:room-compiler:$room_version"
    // If this project only uses Java source, use the Java annotationProcessor
    annotationProcessor "androidx.room:room-compiler:$room_version"
    // optional - RxJava2 support for Room
    implementation "androidx.room:room-rxjava2:$room_version"
    // optional - RxJava3 support for Room
    implementation "androidx.room:room-rxjava3:$room_version"
    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation "androidx.room:room-guava:$room_version"
    // optional - Test helpers
    testImplementation "androidx.room:room-testing:$room_version"
    // optional - Paging 3 Integration
    implementation "androidx.room:room-paging:$room_version"
}
```

</details>

For information on using the KAPT plugin, see the [KAPT documentation](https://kotlinlang.org/docs/kapt.html).

For information on using the KSP plugin, see the [KSP quick-start documentation](https://developer.android.com/build/migrate-to-ksp).

For information on using Kotlin extensions, see the [ktx documentation](https://developer.android.com/kotlin/ktx).

For more information about dependencies, see [Add Build Dependencies](https://developer.android.com/studio/build/dependencies).

Optionally, for non-Android libraries (i.e. Java or Kotlin only Gradle modules)
you can depend on androidx.room:room-common to use Room annotations.

## Configuring Compiler Options

Room has the following annotation processor options.

| Option | Description |
| --- | --- |
| `room.schemaLocation` | directory. Enables exporting database schemas into JSON files in the given directory. See [Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-schemas) for more information. |
| `room.incremental` | boolean. Enables Gradle incremental annotation processor. Default value is true. |
| `room.generateKotlin` | boolean. Generate Kotlin source files instead of Java. Requires KSP. Default value is true as of version 2.7.0. See [version 2.6.0 notes](https://developer.android.com/jetpack/androidx/releases/room#2.6.0), when it was introduced, for more details. |

## Use the Room Gradle Plugin

With Room version 2.6.0 and higher, you can use the Room Gradle Plugin to
configure options for the Room compiler. The plugin configures the project such
that generated schemas (which are an output of the compile tasks and are
consumed for auto-migrations) are correctly configured to have reproducible and
cacheable builds.

To add the plugin, in your top-level Gradle build file, define the
plugin and its version.

### Kotlin
```kotlin
plugins {
    id("androidx.room") version "$room_version" apply false
}
```

<details>
<summary><b>Groovy</b></summary>

```groovy
plugins {
    id 'androidx.room' version "$room_version" apply false
}
```

</details>

In the module-level Gradle build file, apply the plugin and use the `room`
extension.

### Kotlin
```kotlin
plugins {
    id("androidx.room")
}

android {
    // ...
    room {
        schemaDirectory("$projectDir/schemas")
    }
}
```

<details>
<summary><b>Groovy</b></summary>

```groovy
plugins {
    id 'androidx.room'
}

android {
    // ...
    room {
        schemaDirectory "$projectDir/schemas"
    }
}
```

</details>

Setting a `schemaDirectory` is required when using the Room Gradle Plugin. This
will configure the Room compiler and the various compile tasks and its backends
(javac, KAPT, KSP) to output schema files into flavored folders, for example
`schemas/flavorOneDebug/com.package.MyDatabase/1.json`. These files should be
checked into the repository to be used for validation and auto-migrations.

Some options cannot be configured in all versions of the Room Gradle Plugin,
even though they are supported by the Room compiler. The table below lists each
option and shows the version of the Room Gradle Plugin that added support for
configuring that option using the `room` extension. If your version is lower,
or if the option is not supported yet, you can use
[annotation processor options](#use-annotation-processor-options) instead.

| Option | Since version |
| --- | --- |
| `room.schemaLocation` (required) | 2.6.0 |
| `room.incremental` | - |
| `room.generateKotlin` | - |

## Use annotation processor options

If you aren't using the Room Gradle Plugin, or if the option you want isn't
supported by your version of the plugin, you can configure Room using
annotation processor options, as described in
[Add build dependencies](https://developer.android.com/studio/build/dependencies). How you
specify annotation options depends on whether you use KSP or KAPT for Room.

### Kotlin
```kotlin
// For KSP
ksp {
    arg("option_name", "option_value")
    // other options...
}

// For javac and KAPT
android {
    // ...
    defaultConfig {
        // ...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "option_name" to "option_value",
                    // other options...
                )
            }
        }
    }
}
```

<details>
<summary><b>Groovy</b></summary>

```groovy
// For KSP
ksp {
    arg("option_name", "option_value")
    // other options...
}

// For javac and KAPT
android {
    // ...
    defaultConfig {
        // ...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += [
                    "option_name":"option_value",
                    // other options...
                ]
            }
        }
    }
}
```

</details>

Because `room.schemaLocation` is a directory and not a primitive type, it is
necessary to use a `CommandLineArgumentsProvider` when adding this option so
that Gradle knows about this directory when conducting up-to-date checks.
[Migrate your Room database](https://developer.android.com/training/data-storage/room/migrating-db-versions#set_schema_location_using_annotation_processor_option)
shows a complete implementation of `CommandLineArgumentsProvider` that provides
the schema location.

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:413107)
