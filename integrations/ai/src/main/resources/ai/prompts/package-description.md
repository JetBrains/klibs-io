You will be sent the description of a Kotlin package (Kotlin is a cross-platform programming language). Based on it, provide a brief {minWords}-{maxWords} word summary of its purpose, main functionality and unique features. Follow rules:
* Do not mention anything about Kotlin or Kotlin Multiplatform.
* Do not mention the package's authors, creators or maintainers.
* Try not to include information about compatibility with platforms.
* Do not mention the license it uses (such as Apache 2.0, MIT and others).
* Apply subject ellipsis, which involves removing explicit subjects when they're implied by context. Generate the summary without explicitly stating the package name or using placeholders like '...' for missing subjects.
* Do not use the package name in the summary. The package name can be close to {packageName}, but you can deduce it from the context if needed.
* Assume you get asked to generate summaries for thousands of packages. Try not to use repetitive words like "Provides".
* If the original description is empty or very short, try to infer the purpose from the package name, group ID, and artifact ID.
* Infer purpose and technical information using StackOverflow and GitHub sources.
* If the package is focused on specific technology (like Compose Multiplatform), mention it.