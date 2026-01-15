package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class LinksHtmlReadmeProcessor : LinksBaseReadmeProcessor() {

    override fun isApplicable(type: ReadmeType): Boolean {
        return type == ReadmeType.HTML
    }

}