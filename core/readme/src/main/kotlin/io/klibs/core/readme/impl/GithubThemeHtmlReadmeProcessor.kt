package io.klibs.core.readme.impl

import io.klibs.core.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class GithubThemeHtmlReadmeProcessor : GithubThemeBaseReadmeProcessor() {

    override fun isApplicable(type: ReadmeType): Boolean = (type == ReadmeType.HTML)

}