<div class="markdown-heading"><h1 class="heading-element">Navigation</h1><a id="user-content-navigation" class="anchor" aria-label="Permalink: Navigation" href="#navigation"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Navigation is a framework for navigating between 'destinations' within an Android application that provides a consistent API whether destinations are implemented as Fragments, Activities, or other components.</p>
<p>[<a href="https://developer.android.com/guide/navigation" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://github.com/android/architecture-components-samples">Code Sample</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/navigation/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Navigation, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>plugins {
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
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>plugins {
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
</code></pre>
</details>
<div class="markdown-heading"><h2 class="heading-element">Safe Args</h2><a id="user-content-safe-args" class="anchor" aria-label="Permalink: Safe Args" href="#safe-args"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add Safe Args to your project, include the following <code>classpath</code> in your top level <code>build.gradle</code> file:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-1" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-1"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>buildscript {
  repositories {
    google()
  }
  dependencies {
    val nav_version = "2.9.6"
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
  }
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>buildscript {
  repositories {
    google()
  }
  dependencies {
    def nav_version = "2.9.6"
    classpath "androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version"
  }
}
</code></pre>
</details>
<p>You must also apply one of two available plugins.</p>
<p>To generate Java language code suitable for Java or mixed Java and Kotlin modules, add this line to <strong>your app or module's</strong> <code>build.gradle</code> file:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-2" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-2"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>plugins {
  id("androidx.navigation.safeargs")
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>plugins {
  id 'androidx.navigation.safeargs'
}
</code></pre>
</details>
<p>Alternatively, to generate Kotlin code suitable for Kotlin-only modules, add:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-3" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-3"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>plugins {
  id("androidx.navigation.safeargs.kotlin")
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>plugins {
  id 'androidx.navigation.safeargs.kotlin'
}
</code></pre>
</details>
<p>You must have <code>android.useAndroidX=true</code> in your <a href="https://developer.android.com/studio/build#properties-files" rel="nofollow"><code>gradle.properties</code> file</a> as per <a href="https://developer.android.com/jetpack/androidx/migrate" rel="nofollow">Migrating to AndroidX</a>.</p>
<p>For information on using Kotlin extensions, see the <a href="https://developer.android.com/kotlin/ktx" rel="nofollow">ktx documentation</a>.</p>
<p>For more information about dependencies, see <a href="https://developer.android.com/studio/build/dependencies" rel="nofollow">Add Build Dependencies</a>.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:409828" rel="nofollow">Issue Tracker</a></p>
