package io.klibs.app.service

interface TagsGenerationService {
    fun generateTagsForProject(
        projectName: String,
        projectDescription: String,
        repoDescription: String,
        readmeMdContent: String
    ): List<String>
}