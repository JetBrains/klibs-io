"use client"

import R1 from "@/app/img/kodee/r1.svg";
import R2 from "@/app/img/kodee/r2.svg";
import R3 from "@/app/img/kodee/r3.svg";

import Image from "next/image";
import Link from "next/link";
import Container from "@/app/ui/container";
import {textCn} from "@rescui/typography";
import styles from "./styles.module.css";
import cn from "classnames";

const SUBMIT_ISSUE_URL =
    "https://github.com/JetBrains/klibs-io-issue-management/issues/new?assignees=&labels=question&projects=&template=question.md&title=";
const POM_EXAMPLE_URL =
    "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0.pom";
const TOOLING_METADATA_EXAMPLE_URL =
    "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0-kotlin-tooling-metadata.json";

export default function Faq() {
    return (
        <Container
            mode={"container"}
            className={textCn("rs-text-2", {hardness: "hard"})}
        >
            <Container mode="wrapper" className={cn("padding-bottom-large padding-top-medium", styles.textWrapper)}>

                <h1 className={cn(textCn("rs-h1"), "padding-top-medium")}>FAQ</h1>

                <Image
                    src={R1}
                    alt="Backgound for Kodee"
                    className={cn(styles.img1, "hide-on-small hide-on-medium")}
                />
                <Image
                    src={R2}
                    alt="Kodee"
                    className={cn(styles.img2, "hide-on-small hide-on-medium")}
                />
                <Image
                    src={R3}
                    alt="Foreground for Kodee"
                    className={cn(styles.img3, "hide-on-small hide-on-medium")}
                />

                <Container mode="wrapper" className={styles.questionWrapper}>

                    <h4>What is klibs.io?</h4>
                    <p>
                        klibs.io is a search platform and catalog for Kotlin Multiplatform (KMP) libraries. It helps
                        developers discover Kotlin libraries that work across multiple platforms such as Android, iOS,
                        JVM, JS, and WASM. The website is designed to make library evaluation easier by bringing
                        together KMP-related information in one place.
                        <br/><br/>

                        When library metadata is incomplete, klibs.io may generate additional metadata for libraries
                        using AI to improve search and their discoverability. However, the content of libraries is not
                        used to train or fine-tune language models.

                        <br/><br/>

                        Join <a href="#slack-guide">#klibs-io</a> on Slack to follow updates!
                    </p>


                    <h4 id='how-do-i-add-a-project'>How can I submit my own KMP library or project to be listed?</h4>
                    <div>
                        <p>
                            Projects are added automatically within <b>one month</b> (it is a frequency
                            of <a href="https://repo1.maven.org/maven2/.index/">maven central public index</a> update)
                            if they meet the following criteria:
                        </p>

                        <ul>
                            <li>The project is open source and is available on GitHub.</li>
                            <li>At least one artifact is published to Maven Central.</li>
                            <li>
                                At least one artifact is multiplatform
                                must have <a href={TOOLING_METADATA_EXAMPLE_URL} target="_blank"
                                             className={"link-secondary"}>kotlin-tooling-metadata.json</a>.
                            </li>
                            <li>
                                At least one artifact&apos;s POM contains a <b>valid</b> link to the GitHub
                                repository,
                                either under &quot;url&quot; or &quot;scm.url&quot; <a
                                href={POM_EXAMPLE_URL} target="_blank"
                                className={"link-secondary"}>example</a>.
                            </li>
                        </ul>

                        <p>
                            If your project is already presented in the klibs.io,
                            then new versions should appear the <b>next day</b> after
                            they are published to Maven Central.
                        </p>

                        <p>
                            If you believe your project satisfies all criteria, yet it&apos;s still not
                            available, or new versions are not appearing,
                            please <a href={SUBMIT_ISSUE_URL} target="_blank" className={"link-secondary"}>submit
                            an issue</a>.
                        </p>
                    </div>

                    <h4>How are libraries selected or ranked on klibs.io?</h4>
                    <p>
                        Libraries on klibs.io are discovered from public package repositories and code hosting
                        platforms. Ranking may consider factors such as relevance to your query, popularity, and project
                        activity. The ranking logic can evolve over time as the platform improves.
                    </p>

                    <h4>Are there any licensing or usage considerations when using libraries listed on klibs.io?</h4>
                    <p>
                        klibs.io is only a discovery tool. When you use a library, you must follow that library’s
                        license terms, just as if you had found it directly on GitHub or Maven Central.
                    </p>

                    <h4>Where can I find tutorials or examples of using KMP libraries from klibs.io?</h4>
                    <p>
                        If you are looking for tutorials or example projects that demonstrate how Kotlin Multiplatform
                        libraries can be used in practice, see the curated list of Kotlin Multiplatform samples in <a
                        href={'https://kotlinlang.org/docs/multiplatform/multiplatform-samples.html'}>the Kotlin
                        documentation</a>. These examples show common project structures and practical use cases
                        across multiple platforms.
                    </p>

                    <h4>Will GitLab/Bitbucket be indexed?</h4>
                    <p>
                        GitLab and Bitbucket should be supported at some point, but for now we want to focus
                        on validating our ideas and iterating quickly. Adding support for GitLab and Bitbucket
                        would slow us down, while not giving much in return, as 95% of KMP projects already use
                        GitHub.
                    </p>


                    <h4>Will Gradle plugins be indexed?</h4>
                    <p>
                        Gradle Plugins should be indexed at some point as they provide a lot of value to Kotlin
                        Multiplatform projects and libraries. They will likely have a separate category
                        like &quot;Tools&quot;.
                    </p>


                    <h4>What about Java/JVM-only packages?</h4>
                    <p>
                        We would like to add them in the future.
                    </p>

                </Container>

                <Container mode="wrapper" className={cn("padding-bottom-large", styles.questionWrapper)}>
                    <h2 className={textCn("rs-h2")}>How can I provide feedback or report an issue?</h2>

                    <br />

                    <p>
                        You can provide feedback or report issues with klibs.io in a few ways.
                    </p>

                    <h4>
                        Open a GitHub issue
                    </h4>
                    <p>
                        The main place to report problems (bugs, missing libraries, incorrect metadata) is the <a
                        href={'https://github.com/JetBrains/klibs-io/issues/new/choose'}>GitHub issue tracker</a>.
                    </p>

                    <h4>
                        Join the community discussion
                    </h4>
                    <p>
                        If you are a member of Kotlin public Slack, <Link target="_blank"
                                                                          href={"https://kotlinlang.slack.com/archives/C081AF4JK70"}>join
                        the #klibs-io channel</Link>.

                        <br/>

                        If not, become a member via <Link target="_blank"
                                                          href={"https://surveys.jetbrains.com/s3/kotlin-slack-sign-up"}>application
                        form</Link>.
                    </p>

                    <h4>
                        Who maintains klibs.io
                    </h4>
                    <p>
                        klibs.io was originally created by <a href={'https://x.com/IgnatBeresnev'}>Ignat
                        Beresnev</a> and is currently developed and maintained by
                        the
                        Kotlin Websites team at JetBrains. To learn more about the motivation behind the project,
                        watch <a href={'https://www.youtube.com/watch?v=rKbM3e0OidI'}>Ignat’s talk</a> at KotlinConf
                        ‘25.
                    </p>
                </Container>

            </Container>

        </Container>

    );
}
