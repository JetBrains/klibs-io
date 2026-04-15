<div class="markdown-heading"><h1 class="heading-element">Paging</h1><a id="user-content-paging" class="anchor" aria-label="Permalink: Paging" href="#paging"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>The Paging Library makes it easier for you to load data gradually and gracefully within your app's RecyclerView.</p>
<p>[<a href="https://developer.android.com/topic/libraries/architecture/paging/v3-overview" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://github.com/android/architecture-components-samples">Code Sample</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/paging/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Paging, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>dependencies {
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
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>dependencies {
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
</code></pre>
</details>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:413106" rel="nofollow">Issue Tracker</a></p>
