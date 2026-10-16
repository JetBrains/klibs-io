import type { Metadata, ResolvingMetadata } from "next";
import { socialMetadata } from "@/app/helpers";
import React from "react";

export async function generateMetadata(_: unknown, parent: ResolvingMetadata): Promise<Metadata> {
    const title = `Klibs.io FAQ - Kotlin Multiplatform Libraries Help`;
    const description = 'Find answers to common questions about Klibs.io and Kotlin Multiplatform libraries.';

    return {
        title: { absolute: title },
        description,
        ...await socialMetadata(parent, title, description),
    };
}

const SUBMIT_ISSUE_URL =
    "https://github.com/JetBrains/klibs-io-issue-management/issues/new?assignees=&labels=question&projects=&template=question.md&title=";
const REQUEST_INDEXING_URL =
    "https://github.com/JetBrains/klibs-io-issue-management/issues/new?assignees=&labels=index-request&projects=&template=index_request.yml";
const GOOGLE_MAVEN_CENTRAL_MIRROR_URL =
    "https://storage-download.googleapis.com/maven-central/index.html";
const POM_EXAMPLE_URL =
    "https://maven-central.storage-download.googleapis.com/maven2/" +
    "org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0.pom";
const TOOLING_METADATA_EXAMPLE_URL =
    "https://maven-central.storage-download.googleapis.com/maven2/" +
    "org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.0/kotlinx-coroutines-core-1.8.0-kotlin-tooling-metadata.json";
const INDEX_PROPERTIES_URL =
    "https://maven-central.storage-download.googleapis.com/maven2/.index/nexus-maven-repository-index.properties";

const faqSchema = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    "mainEntity": [
        {
            "@type": "Question",
            "name": "What is klibs.io?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "klibs.io is a search platform and catalog for Kotlin Multiplatform (KMP) libraries. It helps developers discover Kotlin libraries that work across multiple platforms such as Android, iOS, JVM, JS, and WASM. The website is designed to make library evaluation easier by bringing together KMP-related information in one place. When library metadata is incomplete, klibs.io may generate additional metadata for libraries using AI to improve search and their discoverability. However, the content of libraries is not used to train or fine-tune language models. Join #klibs-io on Slack to follow updates!"
            }
        },
        {
            "@type": "Question",
            "name": "How can I submit my own KMP library or project to be listed?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text":
                    "For a project to be listed on klibs.io, it must first meet the following criteria: " +
                    "The project is open source and is available on GitHub. " +
                    "At least one artifact is published to Maven Central and the exact version is available " +
                    `on <a href="${GOOGLE_MAVEN_CENTRAL_MIRROR_URL}">Google's Maven Central mirror</a>. ` +
                    "At least one artifact is multiplatform – must have " +
                    `<a href="${TOOLING_METADATA_EXAMPLE_URL}">kotlin-tooling-metadata.json</a>. ` +
                    "At least one artifact's POM contains a valid link to the GitHub repository, " +
                    `either under "url" or "scm.url" <a href="${POM_EXAMPLE_URL}">POM example</a>. ` +
                    "Publish your library to Maven Central as usual, not directly to Google's mirror. " +
                    "Before waiting for indexing or reporting a missing version, verify that the exact " +
                    "version's POM and kotlin-tooling-metadata.json are available on Google's " +
                    "Maven Central mirror. Availability on Maven Central alone is not enough. " +
                    "Mirror propagation can take time, there is no guaranteed synchronization deadline. " +
                    "klibs.io checks new versions of already-listed projects daily. The " +
                    `<a href="${INDEX_PROPERTIES_URL}">mirrored index properties</a> identify the available index. ` +
                    "If the exact version is available on the mirror and you prefer not to wait for automatic " +
                    `discovery, you can <a href="${REQUEST_INDEXING_URL}">submit an indexing request</a>. ` +
                    "Manual indexing cannot fetch files that are absent from the mirror. " +
                    "If your project meets all criteria and the exact version is available on Google's " +
                    "mirror, but it is still missing after the discovery cycle, " +
                    `please <a href="${SUBMIT_ISSUE_URL}">submit an issue</a>.`
            }
        },
        {
            "@type": "Question",
            "name": "How can I check if my project is queued for indexing?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "You can check the indexing status of any artifact at https://klibs.io/package/GROUP_ID/ARTIFACT_ID/VERSION/status, by replacing GROUP_ID, ARTIFACT_ID, and VERSION with the corresponding values from the project."
            }
        },
        {
            "@type": "Question",
            "name": "How are libraries selected or ranked on klibs.io?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "Libraries on klibs.io are discovered from public package repositories and code hosting platforms. Ranking may consider factors such as relevance to your query, popularity, and project activity. The ranking logic can evolve over time as the platform improves."
            }
        },
        {
            "@type": "Question",
            "name": "Are there any licensing or usage considerations when using libraries listed on klibs.io?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "klibs.io is only a discovery tool. When you use a library, you must follow that library's license terms, just as if you had found it directly on GitHub or Maven Central."
            }
        },
        {
            "@type": "Question",
            "name": "Where can I find tutorials or examples of using KMP libraries from klibs.io?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "If you are looking for tutorials or example projects that demonstrate how Kotlin Multiplatform libraries can be used in practice, see the curated list of Kotlin Multiplatform samples in the Kotlin documentation. These examples show common project structures and practical use cases across multiple platforms."
            }
        },
        {
            "@type": "Question",
            "name": "Will GitLab/Bitbucket be indexed?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "GitLab and Bitbucket should be supported at some point, but for now we want to focus on validating our ideas and iterating quickly. Adding support for GitLab and Bitbucket would slow us down, while not giving much in return, as 95% of KMP projects already use GitHub."
            }
        },
        {
            "@type": "Question",
            "name": "Will Gradle plugins be indexed?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "Gradle Plugins should be indexed at some point as they provide a lot of value to Kotlin Multiplatform projects and libraries. They will likely have a separate category like \"Tools\"."
            }
        },
        {
            "@type": "Question",
            "name": "What about Java/JVM-only packages?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "We would like to add them in the future."
            }
        },
        {
            "@type": "Question",
            "name": "How can I provide feedback or report an issue?",
            "acceptedAnswer": {
                "@type": "Answer",
                "text": "You can provide feedback or report issues with klibs.io in a few ways. Open a GitHub issue: The main place to report problems (bugs, missing libraries, incorrect metadata) is the GitHub issue tracker. Join the community discussion: If you are a member of Kotlin public Slack, join the #klibs-io channel. If not, become a member via application form. klibs.io was originally created by Ignat Beresnev and is currently developed and maintained by the Kotlin Websites team at JetBrains."
            }
        }
    ]
};

export default function FaqLayout({ children }: { children: React.ReactNode }) {
    return (
        <>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }}
            />
            {children}
        </>
    );
}
