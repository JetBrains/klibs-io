<div class="markdown-heading"><h1 class="heading-element">Collection</h1><a id="user-content-collection" class="anchor" aria-label="Permalink: Collection" href="#collection"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Reduce the memory impact of existing and new collections that are small.</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/collection/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Collection, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<pre><code>dependencies {
    val collection_version = "1.5.0"
    implementation("androidx.collection:collection:$collection_version")
}
</code></pre>
<details>
<summary><b>Groovy</b></summary>
<pre><code>dependencies {
    def collection_version = "1.5.0"
    implementation "androidx.collection:collection:$collection_version"
}
</code></pre>
</details>
<p>For more information about dependencies, see Add Build Dependencies.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:460756" rel="nofollow">Issue Tracker</a></p>
