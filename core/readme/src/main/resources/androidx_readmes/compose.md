<div class="markdown-heading"><h1 class="heading-element">Compose</h1><a id="user-content-compose" class="anchor" aria-label="Permalink: Compose" href="#compose"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Define your UI programmatically with composable functions that describe its shape and data dependencies.</p>
<p>[<a href="https://developer.android.com/jetpack/compose/tutorial" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://github.com/android/compose-samples">Code Sample</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Structure</h2><a id="user-content-structure" class="anchor" aria-label="Permalink: Structure" href="#structure"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Compose is combination of 7 Maven Group Ids within <code>androidx</code>. Each Group contains a targeted subset of functionality, each with its own set of release notes.
This table explains the groups and links to each set of release notes.</p>
<table>
<thead>
<tr>
<th>Group</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-animation" rel="nofollow">androidx.compose.animation</a></td>
<td>Build animations in their Jetpack Compose applications to enrich the user experience.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-compiler" rel="nofollow">androidx.compose.compiler</a></td>
<td>Transform @Composable functions and enable optimizations with a Kotlin compiler plugin.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-foundation" rel="nofollow">androidx.compose.foundation</a></td>
<td>Write Jetpack Compose applications with ready to use building blocks and extend foundation to build your own design system pieces.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-material" rel="nofollow">androidx.compose.material</a></td>
<td>Build Jetpack Compose UIs with ready to use Material Design Components. This is the higher level entry point of Compose, designed to provide components that match those described at <a href="http://www.material.io" rel="nofollow">www.material.io</a>.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-material3" rel="nofollow">androidx.compose.material3</a></td>
<td>Build Jetpack Compose UIs with Material Design 3 Components, the next evolution of Material Design. Material 3 includes updated theming and components and Material You personalization features like dynamic color, and is designed to be cohesive with the new Android 12 visual style and system UI.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-runtime" rel="nofollow">androidx.compose.runtime</a></td>
<td>Fundamental building blocks of Compose's programming model and state management, and core runtime for the Compose Compiler Plugin to target.</td>
</tr>
<tr>
<td><a href="https://developer.android.com/jetpack/androidx/releases/compose-ui" rel="nofollow">androidx.compose.ui</a></td>
<td>Fundamental components of compose UI needed to interact with the device, including layout, drawing, and input.</td>
</tr>
</tbody>
</table>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Compose, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>android {
    buildFeatures {
        compose <span class="pl-k">=</span> <span class="pl-c1">true</span>
    }
    composeOptions {
        kotlinCompilerExtensionVersion <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>1.5.15<span class="pl-pds">"</span></span>
    }
    kotlinOptions {
        jvmTarget <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>1.8<span class="pl-pds">"</span></span>
    }
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>android {
    buildFeatures {
        compose <span class="pl-c1">true</span>
    }
    composeOptions {
        kotlinCompilerExtensionVersion <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>1.5.15<span class="pl-pds">"</span></span>
    }
    kotlinOptions {
        jvmTarget <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>1.8<span class="pl-pds">"</span></span>
    }
}</pre></div>
</details>
<p>For more information about dependencies, see <a href="https://developer.android.com/studio/build/dependencies" rel="nofollow">Add build dependencies</a>.</p>
<div class="markdown-heading"><h2 class="heading-element">BOMs</h2><a id="user-content-boms" class="anchor" aria-label="Permalink: BOMs" href="#boms"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>For the latest BOM releases, visit <a href="https://developer.android.com/jetpack/compose/bom/bom-mapping" rel="nofollow">Compose BOM Mapping Page</a>.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:610764" rel="nofollow">Issue Tracker</a></p>
