<div class="markdown-heading"><h1 class="heading-element">Room</h1><a id="user-content-room" class="anchor" aria-label="Permalink: Room" href="#room"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite.</p>
<p>[<a href="https://developer.android.com/training/data-storage/room" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/room/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on Room, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>Add the dependencies for the artifacts you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    <span class="pl-k">val</span> room_version <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>2.8.4<span class="pl-pds">"</span></span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-runtime:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)</span>
    ksp(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-compiler:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - Kotlin Extensions and Coroutines support for Room</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-ktx:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support for Room</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-rxjava2:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support for Room</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-rxjava3:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - Guava support for Room, including Optional and ListenableFuture</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-guava:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - Test helpers</span>
    testImplementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-testing:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> optional - Paging 3 Integration</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-paging:<span class="pl-e">$room_version</span><span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    <span class="pl-k">def</span> room_version <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>2.8.4<span class="pl-pds">"</span></span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-runtime:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)</span>
    ksp <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-compiler:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> If this project only uses Java source, use the Java annotationProcessor</span>
    annotationProcessor <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-compiler:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support for Room</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-rxjava2:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support for Room</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-rxjava3:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> optional - Guava support for Room, including Optional and ListenableFuture</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-guava:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> optional - Test helpers</span>
    testImplementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-testing:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
    <span class="pl-c"><span class="pl-c">//</span> optional - Paging 3 Integration</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.room:room-paging:<span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span>
}</pre></div>
</details>
<p>For information on using the KAPT plugin, see the <a href="https://kotlinlang.org/docs/kapt.html" rel="nofollow">KAPT documentation</a>.</p>
<p>For information on using the KSP plugin, see the <a href="https://developer.android.com/build/migrate-to-ksp" rel="nofollow">KSP quick-start documentation</a>.</p>
<p>For information on using Kotlin extensions, see the <a href="https://developer.android.com/kotlin/ktx" rel="nofollow">ktx documentation</a>.</p>
<p>For more information about dependencies, see <a href="https://developer.android.com/studio/build/dependencies" rel="nofollow">Add Build Dependencies</a>.</p>
<p>Optionally, for non-Android libraries (i.e. Java or Kotlin only Gradle modules)
you can depend on androidx.room:room-common to use Room annotations.</p>
<div class="markdown-heading"><h2 class="heading-element">Configuring Compiler Options</h2><a id="user-content-configuring-compiler-options" class="anchor" aria-label="Permalink: Configuring Compiler Options" href="#configuring-compiler-options"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Room has the following annotation processor options.</p>
<table>
<thead>
<tr>
<th>Option</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>room.schemaLocation</code></td>
<td>directory. Enables exporting database schemas into JSON files in the given directory. See <a href="https://developer.android.com/training/data-storage/room/migrating-db-schemas" rel="nofollow">Room Migrations</a> for more information.</td>
</tr>
<tr>
<td><code>room.incremental</code></td>
<td>boolean. Enables Gradle incremental annotation processor. Default value is true.</td>
</tr>
<tr>
<td><code>room.generateKotlin</code></td>
<td>boolean. Generate Kotlin source files instead of Java. Requires KSP. Default value is true as of version 2.7.0. See <a href="https://developer.android.com/jetpack/androidx/releases/room#2.6.0" rel="nofollow">version 2.6.0 notes</a>, when it was introduced, for more details.</td>
</tr>
</tbody>
</table>
<div class="markdown-heading"><h2 class="heading-element">Use the Room Gradle Plugin</h2><a id="user-content-use-the-room-gradle-plugin" class="anchor" aria-label="Permalink: Use the Room Gradle Plugin" href="#use-the-room-gradle-plugin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>With Room version 2.6.0 and higher, you can use the Room Gradle Plugin to
configure options for the Room compiler. The plugin configures the project such
that generated schemas (which are an output of the compile tasks and are
consumed for auto-migrations) are correctly configured to have reproducible and
cacheable builds.</p>
<p>To add the plugin, in your top-level Gradle build file, define the
plugin and its version.</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-1" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-1"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>androidx.room<span class="pl-pds">"</span></span>) version <span class="pl-s"><span class="pl-pds">"</span><span class="pl-e">$room_version</span><span class="pl-pds">"</span></span> apply <span class="pl-c1">false</span>
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>plugins {
    id <span class="pl-s"><span class="pl-pds">'</span>androidx.room<span class="pl-pds">'</span></span> version <span class="pl-s"><span class="pl-pds">"</span><span class="pl-smi">$r<span class="pl-smi">oom_version</span></span><span class="pl-pds">"</span></span> apply <span class="pl-c1">false</span>
}</pre></div>
</details>
<p>In the module-level Gradle build file, apply the plugin and use the <code>room</code>
extension.</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-2" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-2"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>androidx.room<span class="pl-pds">"</span></span>)
}

android {
    <span class="pl-c"><span class="pl-c">//</span> ...</span>
    room {
        schemaDirectory(<span class="pl-s"><span class="pl-pds">"</span><span class="pl-e">$projectDir</span>/schemas<span class="pl-pds">"</span></span>)
    }
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>plugins {
    id <span class="pl-s"><span class="pl-pds">'</span>androidx.room<span class="pl-pds">'</span></span>
}

android {
    <span class="pl-c"><span class="pl-c">//</span> ...</span>
    room {
        schemaDirectory <span class="pl-s"><span class="pl-pds">"</span><span class="pl-smi">$p<span class="pl-smi">rojectDir</span></span>/schemas<span class="pl-pds">"</span></span>
    }
}</pre></div>
</details>
<p>Setting a <code>schemaDirectory</code> is required when using the Room Gradle Plugin. This
will configure the Room compiler and the various compile tasks and its backends
(javac, KAPT, KSP) to output schema files into flavored folders, for example
<code>schemas/flavorOneDebug/com.package.MyDatabase/1.json</code>. These files should be
checked into the repository to be used for validation and auto-migrations.</p>
<p>Some options cannot be configured in all versions of the Room Gradle Plugin,
even though they are supported by the Room compiler. The table below lists each
option and shows the version of the Room Gradle Plugin that added support for
configuring that option using the <code>room</code> extension. If your version is lower,
or if the option is not supported yet, you can use
<a href="#use-annotation-processor-options">annotation processor options</a> instead.</p>
<table>
<thead>
<tr>
<th>Option</th>
<th>Since version</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>room.schemaLocation</code> (required)</td>
<td>2.6.0</td>
</tr>
<tr>
<td><code>room.incremental</code></td>
<td>-</td>
</tr>
<tr>
<td><code>room.generateKotlin</code></td>
<td>-</td>
</tr>
</tbody>
</table>
<div class="markdown-heading"><h2 class="heading-element">Use annotation processor options</h2><a id="user-content-use-annotation-processor-options" class="anchor" aria-label="Permalink: Use annotation processor options" href="#use-annotation-processor-options"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>If you aren't using the Room Gradle Plugin, or if the option you want isn't
supported by your version of the plugin, you can configure Room using
annotation processor options, as described in
<a href="https://developer.android.com/studio/build/dependencies" rel="nofollow">Add build dependencies</a>. How you
specify annotation options depends on whether you use KSP or KAPT for Room.</p>
<div class="markdown-heading"><h3 class="heading-element">Kotlin</h3><a id="user-content-kotlin-3" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-3"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre><span class="pl-c"><span class="pl-c">//</span> For KSP</span>
ksp {
    arg(<span class="pl-s"><span class="pl-pds">"</span>option_name<span class="pl-pds">"</span></span>, <span class="pl-s"><span class="pl-pds">"</span>option_value<span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> other options...</span>
}

<span class="pl-c"><span class="pl-c">//</span> For javac and KAPT</span>
android {
    <span class="pl-c"><span class="pl-c">//</span> ...</span>
    defaultConfig {
        <span class="pl-c"><span class="pl-c">//</span> ...</span>
        javaCompileOptions {
            annotationProcessorOptions {
                arguments <span class="pl-k">+</span><span class="pl-k">=</span> <span class="pl-c1">mapOf</span>(
                    <span class="pl-s"><span class="pl-pds">"</span>option_name<span class="pl-pds">"</span></span> to <span class="pl-s"><span class="pl-pds">"</span>option_value<span class="pl-pds">"</span></span>,
                    <span class="pl-c"><span class="pl-c">//</span> other options...</span>
                )
            }
        }
    }
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre><span class="pl-c"><span class="pl-c">//</span> For KSP</span>
ksp {
    arg(<span class="pl-s"><span class="pl-pds">"</span>option_name<span class="pl-pds">"</span></span>, <span class="pl-s"><span class="pl-pds">"</span>option_value<span class="pl-pds">"</span></span>)
    <span class="pl-c"><span class="pl-c">//</span> other options...</span>
}

<span class="pl-c"><span class="pl-c">//</span> For javac and KAPT</span>
android {
    <span class="pl-c"><span class="pl-c">//</span> ...</span>
    defaultConfig {
        <span class="pl-c"><span class="pl-c">//</span> ...</span>
        javaCompileOptions {
            annotationProcessorOptions {
                arguments <span class="pl-k">+</span><span class="pl-k">=</span> [
                    <span class="pl-s"><span class="pl-pds">"</span>option_name<span class="pl-pds">"</span></span>:<span class="pl-s"><span class="pl-pds">"</span>option_value<span class="pl-pds">"</span></span>,
                    <span class="pl-c"><span class="pl-c">//</span> other options...</span>
                ]
            }
        }
    }
}</pre></div>
</details>
<p>Because <code>room.schemaLocation</code> is a directory and not a primitive type, it is
necessary to use a <code>CommandLineArgumentsProvider</code> when adding this option so
that Gradle knows about this directory when conducting up-to-date checks.
<a href="https://developer.android.com/training/data-storage/room/migrating-db-versions#set_schema_location_using_annotation_processor_option" rel="nofollow">Migrate your Room database</a>
shows a complete implementation of <code>CommandLineArgumentsProvider</code> that provides
the schema location.</p>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:413107" rel="nofollow">Issue Tracker</a></p>
