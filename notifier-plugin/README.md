# klibs-io-notifier Gradle plugin

The `klibs-io-notifier` plugin notifies [klibs.io](https://klibs.io) when a Kotlin Multiplatform library has been published to Maven Central. It sends the library's Maven coordinates to klibs.io so the library can be indexed without waiting for the regular Maven Central scan.

> [!NOTE]
> The plugin is intended for open-source Kotlin Multiplatform projects. It was designed for projects that use the [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/), but it works with any publishing plugin based on Gradle's `maven-publish` after additional configuration (see. [publishTaskName](#publishtaskname)).

## Table of Contents

- [Prerequisites](#prerequisites)
  - [Minimum supported versions](#minimum-supported-versions)
- [Getting started](#getting-started)
  - [Configure the project for publishing](#1-configure-the-project-for-publishing)
  - [Configure plugin resolution](#2-configure-plugin-resolution)
  - [Apply the plugin](#3-apply-the-plugin)
  - [Publish the library](#4-publish-the-library)
  - [(optional) Run klibs-io-notifier only](#5-optional-run-klibs-io-notifier-only)
  - [(optional) Check the indexing status](#6-optional-check-the-indexing-status-of-your-library)
- [Configuration](#configuration)
  - [apiUrl](#apiurl)
  - [publishTaskName](#publishtaskname)
- [How it works](#how-it-works)

## Prerequisites

To use the klibs-io-notifier plugin in your project, it needs to:

- be a Kotlin Multiplatform library
- use Gradle (Kotlin Toolchain support coming soon) 
- publish to Maven Central with a plugin based on Gradle's `maven-publish` (i.e. one that applies `MavenPublishPlugin`), such as `com.vanniktech.maven.publish`

<!-- TODO: KTL-4938 update information about Kotlin Toolchain-->

### Minimum supported versions
<!-- TODO: KTL-4909 Update the section according to the final decision -->

* JDK 17
* Gradle 9.0.0
* Kotlin Gradle Plugin 2.2.0
* vanniktech gradle-maven-publish-plugin 0.36.0

## Getting started

### 1. Configure the project for publishing

Make sure your library is properly configured for publishing to Maven Central. You can find all the necessary steps in the Kotlin documentation:
* [Setting up library publication](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html)
* [Publishing library to Maven Central](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html)

For your library to be listed on klibs.io, it must meet the [klibs.io criteria](https://klibs.io/faq#how-do-i-add-a-project). Make sure that:
* Your project is open source and is available on GitHub
* At least one artifact's POM contains a valid link to the GitHub repository, either under `url` or `scm.url` (see [example](https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0.pom))

### 2. Configure plugin resolution

<!-- TODO: KTL-4909 Update the section according to the actual deployment -->

The klibs-io-notifier plugin is shared via the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.klibs.notifier).

To use the klibs-io-notifier, add the Gradle Plugin Portal to `pluginManagement.repositories` in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

### 3. Apply the plugin

<!-- TODO: KTL-4909 Update the section according to the actual deployment -->

Add the klibs-io-notifier to the `plugins` block in the `build.gradle.kts` of the module you want to publish:

```kotlin
plugins {
    id("io.klibs.notifier") version "1.0.0"
}
```

### 4. Publish the library
After a successful publishing task, the klibs-io-notifier automatically sends a notification — you just need to run one of the publishing tasks as usual.

The example below uses Vanniktech Gradle Maven Publish Plugin:

#### Manual release mode

To first upload the publications to the Maven Central Publisher Portal, and release them manually later, run the following task:

```bash
./gradlew publishToMavenCentral
```

> [!IMPORTANT]
> In the manual mode, the owner of the library must later confirm the publication on the [Maven Central Publisher Portal](https://central.sonatype.com/publishing/deployments) for it to be released.
> If the owner does not do that within **4 days**, the klibs.io will treat the notification as invalid and will not process it.

#### Automatic release mode

Optionally, you can use the publishing task with the auto-release mode, by [adjusting the configuration](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#publishing-releases) of the project or running the following task:

```bash
./gradlew publishAndReleaseToMavenCentral
```
> [!CAUTION]
> Artifacts released to Maven Central cannot be deleted or modified ([read more](https://central.sonatype.org/faq/can-i-change-a-component/)). Before publishing your project in auto-release mode, make sure everything is fully tested and the artifacts are exactly as you intend them to be.

### 5. (optional) Run klibs-io-notifier only

You can also run the notifier manually:

```
./gradlew notifyKlibsIo
```

### 6. (optional) Check the indexing status of your library

After successfully notifying klibs.io, you can check the indexing status of your artifact at `https://klibs.io/package/<GROUP_ID>/<ARTIFACT_ID>/<VERSION>/status`.

## Configuration

The plugin exposes the following settings through the `klibsIoNotifier` extension.

| Setting                             | Description                                         | Default                                                        |
|-------------------------------------|-----------------------------------------------------|----------------------------------------------------------------|
| [apiUrl](#apiurl)                   | URL of the klibs.io notification receiver           | `https://klibs.io/notify/artifacts`                            |
| [publishTaskName](#publishtaskname) | Name of the publishing task the notifier hooks into | `publishKotlinMultiplatformPublicationToMavenCentralRepository` |

### apiUrl

By default, all notifications are sent to the `https://klibs.io/notify/artifacts`. If you need to use a different URL, you can configure the `klibsIoNotifier` extension:

```kotlin
klibsIoNotifier {
    apiUrl.set("https://your-url")
}
```

### publishTaskName

The notifier works out of the box with the Vanniktech plugin. For other plugins you need to configure the `publishTaskName`. Set its value to the name of a Gradle task that you use to publish your library to Maven Central:

```kotlin
klibsIoNotifier {
    publishTaskName.set("yourPublishingTaskName")
}
```

## How it works

When one of the supported Maven Central publishing tasks completes successfully, or when called explicitly, the klibs-io-notifier:

1. reads the `groupId`, `artifactId`, and `version` from the `kotlinMultiplatform` publication
2. verifies that the publication contains `kotlin-tooling-metadata.json`
3. sends the coordinates to the configured [apiUrl](#apiurl) (`POST https://klibs.io/notify/artifacts` by default).

Example of a request body:

```json
{
  "groupId": "com.example",
  "artifactId": "my-library",
  "version": "1.0.0"
}
```

The supported publishing tasks by default are:

- `publishToMavenCentral`
- `publishAndReleaseToMavenCentral`
- `publishKotlinMultiplatformPublicationToMavenCentralRepository` - the task that runs as part of `./gradlew publish`, etc.

The notification task is registered as `notifyKlibsIo` and runs automatically as a finalizer of the hooked publishing task. You can also run it directly (`./gradlew notifyKlibsIo`) to notify klibs.io on demand.

Notification failures are logged as warnings and do not fail the publishing build. This includes network errors and non-2xx responses from klibs.io.

With the Vanniktech plugin, the final Maven Central upload runs at the end of a Gradle build, after the notification has been sent to klibs.io. A successful notification therefore means that the publishing task reached its successful completion point. It does not guarantee that the artifact is already visible in Maven Central.

### Processing of notifications on klibs.io

After receiving the notification from the plugin, klibs.io puts a corresponding indexing request in the queue. It's later fetched by the scheduled job that runs the checks against Maven Central and lists the library on klibs.io.

If the processing of the indexing request fails (for example, because the library was not yet released to the public), the processing is retried three times according to the following schedule:
1. After 4h
2. After 12h
3. After 4 days

If all of these trials also fail, the indexing request is considered failed and won't be retried.
