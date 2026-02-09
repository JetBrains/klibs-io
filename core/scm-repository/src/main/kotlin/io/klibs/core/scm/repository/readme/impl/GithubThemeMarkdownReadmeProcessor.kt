package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class GithubThemeMarkdownReadmeProcessor : GithubThemeBaseReadmeProcessor() {

    private val markdownDarkPattern = Regex("""!\[[^]]*]\([^)]+#gh-dark-mode-only\s*\)""") // images in markdown like ![...](...#gh-dark-mode-only)
    private val markdownLightPattern = Regex("""(!\[[^]]*]\([^)]+)#gh-light-mode-only(\s*\))""") // images in markdown like ![...](...#gh-light-mode-only)

    override fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return super
            .process(readmeContent, readmeOwner, readmeRepositoryName, repositoryDefaultBranch)
            .replace(markdownDarkPattern, "")
            .replace(markdownLightPattern, "$1$2")
     }

    override fun isApplicable(type: ReadmeType): Boolean = (type == ReadmeType.MARKDOWN)

}