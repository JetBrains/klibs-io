package io.klibs.core.project.repository

import io.klibs.core.owner.ScmOwnerType

data class ProjectOwnerRef(
    val login: String,
    val type: ScmOwnerType
)
