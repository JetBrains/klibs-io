package io.klibs.core.project.repository

interface AllowedProjectTagsRepository {
    fun findCanonicalNameByValue(value: String): String?
}