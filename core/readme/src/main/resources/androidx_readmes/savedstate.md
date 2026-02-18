<div class="markdown-heading"><h1 class="heading-element">Savedstate</h1><a id="user-content-savedstate" class="anchor" aria-label="Permalink: Savedstate" href="#savedstate"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Write pluggable components that save the UI state when a process dies, and restore it when the process restarts.</p>
<p>[<a href="https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/savedstate/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Savedstate, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>dependencies {
    // Java language implementation
    implementation("androidx.savedstate:savedstate:1.4.0")

    // Kotlin
    implementation("androidx.savedstate:savedstate-ktx:1.4.0")
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>dependencies {
    // Java language implementation
    implementation "androidx.savedstate:savedstate:1.4.0"

    // Kotlin
    implementation "androidx.savedstate:savedstate-ktx:1.4.0"
}
</code></pre>
</details>
<p>For more information about dependencies, see Add build dependencies.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:878252" rel="nofollow">Issue Tracker</a></p>
