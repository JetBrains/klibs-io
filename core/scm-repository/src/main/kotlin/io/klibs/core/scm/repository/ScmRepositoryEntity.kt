package io.klibs.core.scm.repository

import io.klibs.core.owner.ScmOwnerType
import java.time.Instant

data class ScmRepositoryEntity(
    val id: Int? = null,

    /**
     * ID assigned by the scm, such as GitHub's native ID for the given repository
     */
    val nativeId: Long,

    val name: String,

    val description: String?,
    val defaultBranch: String,

    val createdTs: Instant,

    val ownerId: Int,

    /**
     * Computed from [OwnerEntity.type],
     * no need to populate it when saving/upserting
     */
    val ownerType: ScmOwnerType,

    /**
     * Computed from [OwnerEntity.login],
     * no need to populate it when saving/upserting
     */
    val ownerLogin: String,

    val homepage: String?,

    val hasGhPages: Boolean,
    val hasIssues: Boolean,
    val hasWiki: Boolean,
    val hasReadme: Boolean,

    /**
     * License key used by GitHub and choosealicense.com
     *
     * @see "https://choosealicense.com/licenses/gpl-3.0/"
     * @see "https://docs.github.com/en/rest/licenses/licenses?apiVersion=2022-11-28#get-all-commonly-used-licenses"
     */
    val licenseKey: String?,
    val licenseName: String?,

    val stars: Int,
    val openIssues: Int?,

    val lastActivityTs: Instant,
    val updatedAtTs: Instant,
    var minimizedReadme: String?
) {
    val idNotNull: Int get() = requireNotNull(id)
}
