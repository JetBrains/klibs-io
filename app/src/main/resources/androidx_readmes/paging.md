# Paging

The Paging Library makes it easier for you to load data gradually and gracefully within your app&#39;s RecyclerView. 

[[User Guide](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)] 

[[Code Sample](https://github.com/android/architecture-components-samples)]

[[API Reference](https://developer.android.com/reference/kotlin/androidx/paging/package-summary)]

## Declaring dependencies

To add a dependency on Paging, you must add the Google Maven repository to your project. Read [Google's Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) for more information.

Add the dependencies for the artifacts you need in the `build.gradle` file for your app or module:

### Kotlin
```
dependencies {
  val paging_version = "3.3.6"

  implementation("androidx.paging:paging-runtime:$paging_version")

  // alternatively - without Android dependencies for tests
  testImplementation("androidx.paging:paging-common:$paging_version")

  // optional - RxJava2 support
  implementation("androidx.paging:paging-rxjava2:$paging_version")

  // optional - RxJava3 support
  implementation("androidx.paging:paging-rxjava3:$paging_version")

  // optional - Guava ListenableFuture support
  implementation("androidx.paging:paging-guava:$paging_version")

  // optional - Jetpack Compose integration
  implementation("androidx.paging:paging-compose:3.4.0-rc01")
}
```

<details>
<summary><b>Groovy</b></summary>

```
dependencies {
  def paging_version = "3.3.6"

  implementation "androidx.paging:paging-runtime:$paging_version"

  // alternatively - without Android dependencies for tests
  testImplementation "androidx.paging:paging-common:$paging_version"

  // optional - RxJava2 support
  implementation "androidx.paging:paging-rxjava2:$paging_version"

  // optional - RxJava3 support
  implementation "androidx.paging:paging-rxjava3:$paging_version"

  // optional - Guava ListenableFuture support
  implementation "androidx.paging:paging-guava:$paging_version"

  // optional - Jetpack Compose integration
  implementation "androidx.paging:paging-compose:3.4.0-rc01"
}
```

</details>

## Issue tracker
[Issue Tracker](https://issuetracker.google.com/issues?q=componentid:413106)
