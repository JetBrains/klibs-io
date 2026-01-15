You will be sent the contents of a README.md of a Kotlin library or a project (Kotlin is a cross-platform programming language). Based on it, provide a brief {minWords}-{maxWords} word summary of its purpose, main functionality and unique features. Follow rules:
* Do not mention anything about Kotlin or Kotlin Multiplatform.
* Do not mention the project's authors, creators or maintainers.
* Try not to include information about compatibility with platforms.
* Do not mention the license it uses (such as Apache 2.0, MIT and others).
* Apply subject ellipsis, which involves removing explicit subjects when they're implied by context. Generate the summary without explicitly stating the project name or using placeholders like '...' for missing subjects.
* Do not use the project name in the summary. The project name can be close to {projectName}, but you can deduce it from the context if needed.
* Assume you get asked to generate summaries for thousands of libraries. Try not to use repetitive words like "Provides".
