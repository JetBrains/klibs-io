import type { Metadata } from "next";
import React from "react";

export async function generateMetadata(): Promise<Metadata> {
    const title = `Klibs.io FAQ - Kotlin Multiplatform Libraries Help`;
    const description = 'Find answers to common questions about Klibs.io and Kotlin Multiplatform libraries.';

    return {
        title: { absolute: title },
        description,
        openGraph: { title, description },
        twitter: { title, description },
    };
}

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
                "text": "Projects are added automatically within one month (it is a frequency of maven central public index update) if they meet the following criteria: The project is open source and is available on GitHub. At least one artifact is published to Maven Central. At least one artifact is multiplatform — must have kotlin-tooling-metadata.json. At least one artifact's POM contains a valid link to the GitHub repository, either under \"url\" or \"scm.url\". If your project is already presented in the klibs.io, then new versions should appear the next day after they are published to Maven Central. If you believe your project satisfies all criteria, yet it's still not available, or new versions are not appearing, please submit an issue."
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
