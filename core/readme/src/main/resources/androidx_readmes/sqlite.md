<div class="markdown-heading"><h1 class="heading-element">Sqlite</h1><a id="user-content-sqlite" class="anchor" aria-label="Permalink: Sqlite" href="#sqlite"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>The androidx.sqlite library contains abstract interfaces along with basic implementations which can be used to build your own libraries that access SQLite.  You might want to consider using the Room library, which provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite.</p>
<p>[<a href="https://developer.android.com/training/data-storage/sqlite" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/sqlite/db/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Sqlite, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>dependencies {
    val sqlite_version = "2.6.2"

    // Java language implementation
    implementation("androidx.sqlite:sqlite:$sqlite_version")

    // Kotlin
    implementation("androidx.sqlite:sqlite-ktx:$sqlite_version")

    // Implementation of the AndroidX SQLite interfaces via the Android framework APIs.
    implementation("androidx.sqlite:sqlite-framework:$sqlite_version")
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>dependencies {
    def sqlite_version = "2.6.2"

    // Java language implementation
    implementation "androidx.sqlite:sqlite:$sqlite_version"

    // Kotlin
    implementation "androidx.sqlite:sqlite-ktx:$sqlite_version"

    // Implementation of the AndroidX SQLite interfaces via the Android framework APIs.
    implementation "androidx.sqlite:sqlite-framework:$sqlite_version"
}
</code></pre>
</details>
<p>For more information about dependencies, see Add build dependencies.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:460784" rel="nofollow">Issue Tracker</a></p>
