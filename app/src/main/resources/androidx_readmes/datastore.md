# DataStore

Store data asynchronously, consistently, and transactionally, overcoming some of the drawbacks of SharedPreferences 

[[User Guide](https://developer.android.com/datastore)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/datastore/package-summary)]

## Declaring dependencies

To add a dependency on DataStore, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

DataStore provides [different options for serialization](https://developer.android.com/topic/libraries/architecture/datastore#implementations), choose one or the other. You can also add Android-free dependencies to either implementation.

Add the dependencies for the implementation you need in the `build.gradle` file for your app or module:

### Preferences DataStore

Add the following lines to the dependencies part of your gradle file:

#### Kotlin

```kotlin
dependencies {
    // Preferences DataStore (SharedPreferences like APIs)
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Alternatively - without an Android dependency.
    implementation("androidx.datastore:datastore-preferences-core:1.2.0")
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
dependencies {
    // Preferences DataStore (SharedPreferences like APIs)
    implementation "androidx.datastore:datastore-preferences:1.2.0"

    // Alternatively - without an Android dependency.
    implementation "androidx.datastore:datastore-preferences-core:1.2.0"
}
```

</details>

To add optional RxJava support, add the following dependencies:

#### Kotlin

```kotlin
dependencies {
    // optional - RxJava2 support
    implementation("androidx.datastore:datastore-preferences-rxjava2:1.2.0")

    // optional - RxJava3 support
    implementation("androidx.datastore:datastore-preferences-rxjava3:1.2.0")
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
dependencies {
    // optional - RxJava2 support
    implementation "androidx.datastore:datastore-preferences-rxjava2:1.2.0"

    // optional - RxJava3 support
    implementation "androidx.datastore:datastore-preferences-rxjava3:1.2.0"
}
```

</details>

### DataStore

Add the following lines to the dependencies part of your gradle file:

#### Kotlin

```kotlin
dependencies {
    // Typed DataStore for custom data objects (for example, using Proto or JSON).
    implementation("androidx.datastore:datastore:1.2.0")

    // Alternatively - without an Android dependency.
    implementation("androidx.datastore:datastore-core:1.2.0")
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
dependencies {
    // Typed DataStore for custom data objects (for example, using Proto or JSON).
    implementation "androidx.datastore:datastore:1.2.0"

    // Alternatively - without an Android dependency.
    implementation "androidx.datastore:datastore-core:1.2.0"
}
```

</details>

Add the following optional dependencies for RxJava support:

#### Kotlin

```kotlin
dependencies {
    // optional - RxJava2 support
    implementation("androidx.datastore:datastore-rxjava2:1.2.0")

    // optional - RxJava3 support
    implementation("androidx.datastore:datastore-rxjava3:1.2.0")
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
dependencies {
    // optional - RxJava2 support
    implementation "androidx.datastore:datastore-rxjava2:1.2.0"

    // optional - RxJava3 support
    implementation "androidx.datastore:datastore-rxjava3:1.2.0"
}
```

</details>

To serialize content, add dependencies for either Protocol Buffers or JSON serialization.

#### JSON serialization

To use JSON serialization, add the following to your Gradle file:

##### Kotlin

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
}

dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0"
}
```

</details>

#### Protobuf serialization

To use Protobuf serialization, add the following to your Gradle file:

##### Kotlin

```kotlin
plugins {
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin-lite:4.32.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin")
            }
        }
    }
}
```

<details>
<summary><b>Groovy</b></summary>


```groovy
plugins {
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    implementation "com.google.protobuf:protobuf-kotlin-lite:4.32.1"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin")
            }
        }
    }
}
```

</details>

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:907884)
