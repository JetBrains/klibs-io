package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class GithubThemeHtmlReadmeProcessor : GithubThemeBaseReadmeProcessor() {

    override fun isApplicable(type: ReadmeType): Boolean = (type == ReadmeType.HTML)

}