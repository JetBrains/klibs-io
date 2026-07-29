package io.klibs.core.search.opensearch

/** Field names of the project document, mirroring `project-mappings.json`. */
object ProjectFields {
    const val PROJECT_ID = "project_id"
    const val OWNER_TYPE = "owner_type"
    const val OWNER_LOGIN = "owner_login"
    const val REPO_NAME = "repo_name"
    const val NAME = "name"
    const val GROUP_IDS = "group_ids"
    const val ARTIFACT_IDS = "artifact_ids"
    const val REPO_DESCRIPTION = "repo_description"
    const val PROJECT_DESCRIPTION = "project_description"
    const val PLATFORMS = "platforms"
    const val TAGS = "tags"
    const val MARKERS = "markers"
    const val TARGETS = "targets"
    const val STARS = "stars"
    const val DEPENDENT_COUNT = "dependent_count"
    const val HAS_README = "has_readme"
    const val LICENSE_NAME = "license_name"
    const val LATEST_VERSION = "latest_version"
    const val LATEST_VERSION_TS = "latest_version_ts"
    const val PLAIN_DESCRIPTION = "plain_description"
    const val PACKAGES = "packages"
}

/** Field names of the package document, mirroring `package-mappings.json`. */
object PackageFields {
    const val GROUP_ID = "group_id"
    const val ARTIFACT_ID = "artifact_id"
    const val PROJECT_ID = "project_id"
    const val LATEST_PACKAGE_ID = "latest_package_id"
    const val OWNER_TYPE = "owner_type"
    const val OWNER_LOGIN = "owner_login"
    const val LATEST_DESCRIPTION = "latest_description"
    const val PLATFORMS = "platforms"
    const val TARGETS = "targets"
    const val LATEST_LICENSE_NAME = "latest_license_name"
    const val LATEST_VERSION = "latest_version"
    const val RELEASE_TS = "release_ts"
}

internal val String.keyword: String get() = "$this.keyword"
