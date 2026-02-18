<div class="markdown-heading"><h1 class="heading-element">Annotation</h1><a id="user-content-annotation" class="anchor" aria-label="Permalink: Annotation" href="#annotation"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Expose metadata that helps tools and other developers understand your app's code.</p>
<p>[<a href="https://developer.android.com/reference/androidx/annotation/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Annotation, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.annotation:annotation:1.9.1<span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> To use the Java-compatible @androidx.annotation.OptIn API annotation</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.annotation:annotation-experimental:1.5.1<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.annotation:annotation:1.9.1<span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> To use the Java-compatible @androidx.annotation.OptIn API annotation</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.annotation:annotation-experimental:1.5.1<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<p>For more information about dependencies, see Add build dependencies.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:459778" rel="nofollow">Issue Tracker</a></p>
