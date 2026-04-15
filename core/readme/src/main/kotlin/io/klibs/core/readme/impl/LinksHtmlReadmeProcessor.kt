package io.klibs.core.readme.impl

import io.klibs.core.readme.ReadmeType
import org.springframework.stereotype.Service

@Service
class LinksHtmlReadmeProcessor : LinksBaseReadmeProcessor() {

    override fun isApplicable(type: ReadmeType): Boolean {
        return type == ReadmeType.HTML
    }

}
