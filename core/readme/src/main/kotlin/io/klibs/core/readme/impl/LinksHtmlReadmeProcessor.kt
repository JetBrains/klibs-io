package io.klibs.core.readme.impl

import io.klibs.core.readme.ReadmeType
import io.klibs.core.readme.service.GithubLfsDetector
import org.springframework.stereotype.Service

@Service
class LinksHtmlReadmeProcessor(
    lfsDetector: GithubLfsDetector,
) : LinksBaseReadmeProcessor(
    lfsDetector = lfsDetector,
) {

    override fun isApplicable(type: ReadmeType): Boolean {
        return type == ReadmeType.HTML
    }

}
