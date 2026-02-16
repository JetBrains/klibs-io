package io.klibs.core.project.repository

import io.klibs.core.project.utils.normalizeTag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Repository
import org.yaml.snakeyaml.Yaml

@Repository
class YamlAllowedProjectTagsRepository(
    @Value("classpath:/ai/prompts/tag_rules.yaml")
    private val tagRulesResource: Resource
) : AllowedProjectTagsRepository {

    override fun findCanonicalNameByValue(value: String): String? {
        val normalized = normalizeTag(value)
        if (normalized.isEmpty()) return null
        return allowedTagsBySlug[normalized]
    }

    private val allowedTagsBySlug: Map<String, String> by lazy {
        val yaml = Yaml()

        tagRulesResource.inputStream.use { input ->
            val root = yaml.load<Any>(input) as? Map<*, *> ?: return@use emptyMap()
            val rules = (root["tag_rules"] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()

            val map = LinkedHashMap<String, String>()
            for (rule in rules) {
                val name = rule["name"] as? String
                if (name.isNullOrBlank()) continue

                val normalizedName = normalizeTag(name)
                if (normalizedName.isNotEmpty()) {
                    map.putIfAbsent(normalizedName, name)
                }

                val tagSynonyms = (rule["tag_synonyms"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                for (synonym in tagSynonyms) {
                    val normalizedSynonym = normalizeTag(synonym)
                    if (normalizedSynonym.isNotEmpty()) {
                        map.putIfAbsent(normalizedSynonym, name)
                    }
                }
            }

            map
        }
    }
}