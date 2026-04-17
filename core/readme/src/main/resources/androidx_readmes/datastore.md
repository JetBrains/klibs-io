<div class="markdown-heading"><h1 class="heading-element">DataStore</h1><a id="user-content-datastore" class="anchor" aria-label="Permalink: DataStore" href="#datastore"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Store data asynchronously, consistently, and transactionally, overcoming some of the drawbacks of SharedPreferences</p>
<p>[<a href="https://developer.android.com/datastore" rel="nofollow">User Guide</a>]</p>
<p>[<a href="https://developer.android.com/reference/kotlin/androidx/datastore/package-summary" rel="nofollow">API Reference</a>]</p>
<div class="markdown-heading"><h2 class="heading-element">Declaring dependencies</h2><a id="user-content-declaring-dependencies" class="anchor" aria-label="Permalink: Declaring dependencies" href="#declaring-dependencies"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To add a dependency on DataStore, you must add the Google Maven repository to your project. Read <a href="https://developer.android.com/studio/build/dependencies#google-maven" rel="nofollow">Google's Maven repository</a> for more information.</p>
<p>DataStore provides <a href="https://developer.android.com/topic/libraries/architecture/datastore#implementations" rel="nofollow">different options for serialization</a>, choose one or the other. You can also add Android-free dependencies to either implementation.</p>
<p>Add the dependencies for the implementation you need in the <code>build.gradle</code> file for your app or module:</p>
<div class="markdown-heading"><h3 class="heading-element">Preferences DataStore</h3><a id="user-content-preferences-datastore" class="anchor" aria-label="Permalink: Preferences DataStore" href="#preferences-datastore"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Add the following lines to the dependencies part of your gradle file:</p>
<div class="markdown-heading"><h4 class="heading-element">Kotlin</h4><a id="user-content-kotlin" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> Preferences DataStore (SharedPreferences like APIs)</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences:1.2.0<span class="pl-pds">"</span></span>)

    <span class="pl-c"><span class="pl-c">//</span> Alternatively - without an Android dependency.</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-core:1.2.0<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> Preferences DataStore (SharedPreferences like APIs)</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences:1.2.0<span class="pl-pds">"</span></span>

    <span class="pl-c"><span class="pl-c">//</span> Alternatively - without an Android dependency.</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-core:1.2.0<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<p>To add optional RxJava support, add the following dependencies:</p>
<div class="markdown-heading"><h4 class="heading-element">Kotlin</h4><a id="user-content-kotlin-1" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-1"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-rxjava2:1.2.0<span class="pl-pds">"</span></span>)

    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-rxjava3:1.2.0<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-rxjava2:1.2.0<span class="pl-pds">"</span></span>

    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-preferences-rxjava3:1.2.0<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<div class="markdown-heading"><h3 class="heading-element">DataStore</h3><a id="user-content-datastore-1" class="anchor" aria-label="Permalink: DataStore" href="#datastore-1"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>Add the following lines to the dependencies part of your gradle file:</p>
<div class="markdown-heading"><h4 class="heading-element">Kotlin</h4><a id="user-content-kotlin-2" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-2"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> Typed DataStore for custom data objects (for example, using Proto or JSON).</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore:1.2.0<span class="pl-pds">"</span></span>)

    <span class="pl-c"><span class="pl-c">//</span> Alternatively - without an Android dependency.</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-core:1.2.0<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> Typed DataStore for custom data objects (for example, using Proto or JSON).</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore:1.2.0<span class="pl-pds">"</span></span>

    <span class="pl-c"><span class="pl-c">//</span> Alternatively - without an Android dependency.</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-core:1.2.0<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<p>Add the following optional dependencies for RxJava support:</p>
<div class="markdown-heading"><h4 class="heading-element">Kotlin</h4><a id="user-content-kotlin-3" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-3"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-rxjava2:1.2.0<span class="pl-pds">"</span></span>)

    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support</span>
    implementation(<span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-rxjava3:1.2.0<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>dependencies {
    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava2 support</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-rxjava2:1.2.0<span class="pl-pds">"</span></span>

    <span class="pl-c"><span class="pl-c">//</span> optional - RxJava3 support</span>
    implementation <span class="pl-s"><span class="pl-pds">"</span>androidx.datastore:datastore-rxjava3:1.2.0<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<p>To serialize content, add dependencies for either Protocol Buffers or JSON serialization.</p>
<div class="markdown-heading"><h4 class="heading-element">JSON serialization</h4><a id="user-content-json-serialization" class="anchor" aria-label="Permalink: JSON serialization" href="#json-serialization"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To use JSON serialization, add the following to your Gradle file:</p>
<div class="markdown-heading"><h5 class="heading-element">Kotlin</h5><a id="user-content-kotlin-4" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-4"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>org.jetbrains.kotlin.plugin.serialization<span class="pl-pds">"</span></span>) version <span class="pl-s"><span class="pl-pds">"</span>2.2.20<span class="pl-pds">"</span></span>
}

dependencies {
    implementation(<span class="pl-s"><span class="pl-pds">"</span>org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0<span class="pl-pds">"</span></span>)
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>org.jetbrains.kotlin.plugin.serialization<span class="pl-pds">"</span></span>) version <span class="pl-s"><span class="pl-pds">"</span>2.2.20<span class="pl-pds">"</span></span>
}

dependencies {
    implementation <span class="pl-s"><span class="pl-pds">"</span>org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0<span class="pl-pds">"</span></span>
}</pre></div>
</details>
<div class="markdown-heading"><h4 class="heading-element">Protobuf serialization</h4><a id="user-content-protobuf-serialization" class="anchor" aria-label="Permalink: Protobuf serialization" href="#protobuf-serialization"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p>To use Protobuf serialization, add the following to your Gradle file:</p>
<div class="markdown-heading"><h5 class="heading-element">Kotlin</h5><a id="user-content-kotlin-5" class="anchor" aria-label="Permalink: Kotlin" href="#kotlin-5"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<div class="highlight highlight-source-kotlin"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf<span class="pl-pds">"</span></span>) version <span class="pl-s"><span class="pl-pds">"</span>0.9.5<span class="pl-pds">"</span></span>
}

dependencies {
    implementation(<span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf:protobuf-kotlin-lite:4.32.1<span class="pl-pds">"</span></span>)
}

protobuf {
    protoc {
        artifact <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf:protoc:4.32.1<span class="pl-pds">"</span></span>
    }
    generateProtoTasks {
        all().forEach { task <span class="pl-k">-&gt;</span>
            task.builtins {
                create(<span class="pl-s"><span class="pl-pds">"</span>java<span class="pl-pds">"</span></span>) {
                    option(<span class="pl-s"><span class="pl-pds">"</span>lite<span class="pl-pds">"</span></span>)
                }
                create(<span class="pl-s"><span class="pl-pds">"</span>kotlin<span class="pl-pds">"</span></span>)
            }
        }
    }
}</pre></div>
<details>
<summary><b>Groovy</b></summary>
<div class="highlight highlight-source-groovy"><pre>plugins {
    id(<span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf<span class="pl-pds">"</span></span>) version <span class="pl-s"><span class="pl-pds">"</span>0.9.5<span class="pl-pds">"</span></span>
}

dependencies {
    implementation <span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf:protobuf-kotlin-lite:4.32.1<span class="pl-pds">"</span></span>
}

protobuf {
    protoc {
        artifact <span class="pl-k">=</span> <span class="pl-s"><span class="pl-pds">"</span>com.google.protobuf:protoc:4.32.1<span class="pl-pds">"</span></span>
    }
    generateProtoTasks {
        all()<span class="pl-k">.</span>forEach { <span class="pl-v">task</span> <span class="pl-k">-&gt;</span>
            task<span class="pl-k">.</span>builtins {
                create(<span class="pl-s"><span class="pl-pds">"</span>java<span class="pl-pds">"</span></span>) {
                    option(<span class="pl-s"><span class="pl-pds">"</span>lite<span class="pl-pds">"</span></span>)
                }
                create(<span class="pl-s"><span class="pl-pds">"</span>kotlin<span class="pl-pds">"</span></span>)
            }
        }
    }
}</pre></div>
</details>
<div class="markdown-heading"><h2 class="heading-element">Issue tracker</h2><a id="user-content-issue-tracker" class="anchor" aria-label="Permalink: Issue tracker" href="#issue-tracker"><span aria-hidden="true" class="octicon octicon-link"></span></a></div>
<p><a href="https://issuetracker.google.com/issues?q=componentid:907884" rel="nofollow">Issue Tracker</a></p>
