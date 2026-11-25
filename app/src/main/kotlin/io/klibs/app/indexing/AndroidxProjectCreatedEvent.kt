package io.klibs.app.indexing

import io.klibs.core.readme.dto.AndroidxProjectDTO

data class AndroidxProjectCreatedEvent(
    val project: AndroidxProjectDTO,
)
