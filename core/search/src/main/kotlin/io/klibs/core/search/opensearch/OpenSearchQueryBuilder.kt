package io.klibs.core.search.opensearch

import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.TargetGroup
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Script
import org.opensearch.client.opensearch._types.query_dsl.FunctionBoostMode
import org.opensearch.client.opensearch._types.query_dsl.FunctionScore
import org.opensearch.client.opensearch._types.query_dsl.MatchPhrasePrefixQuery
import org.opensearch.client.opensearch._types.query_dsl.MatchPhraseQuery
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField

object OpenSearchQueryBuilder {

    // Multiplier for the bm25 score for each matching item.
    // - Score is higher if readme is present.
    // - Stars are accounted for with `log`
    // - Dependent_count is accounted for with `log`
    // - Stars have 2.5x more weight than Dependent_count

    private const val POPULARITY_SCRIPT =
        "double d = doc['has_readme'].value ? 1.0 : 0.7; " +
                "return 1 + (" +
                "Math.log(doc['stars'].value + 1) * 0.5 + " +
                "Math.log(doc['dependent_count'].value + 1) * 0.2" +
                ") * d;"

    // Word-bag match: OR over the query terms, docs matching only some of them still score.
    // `text` goes in as a FieldValue, so the serializer quotes it — never concatenated into the DSL.
    fun match(field: String, text: String, boost: Int): Query =
        MatchQuery.Builder()
            .field(field)
            .query(FieldValue.of(text))
            .boost(boost.toFloat())
            .build()
            .toQuery()

    // Same as `match` plus typo tolerance: edit distance up to 2 per term.
    fun fuzzy(field: String, text: String, boost: Int): Query =
        MatchQuery.Builder()
            .field(field)
            .query(FieldValue.of(text))
            .fuzziness("2")
            .boost(boost.toFloat())
            .build()
            .toQuery()

    // Phrase whose LAST term is a prefix: "ktor cli" matches "ktor client".
    fun phrasePrefix(field: String, text: String, boost: Int): Query =
        MatchPhrasePrefixQuery.Builder()
            .field(field)
            .query(text)
            .boost(boost.toFloat())
            .build()
            .toQuery()

    // All terms, order matters, no gaps.
    fun phrase(field: String, text: String, boost: Int): Query =
        MatchPhraseQuery.Builder()
            .field(field)
            .query(text)
            .boost(boost.toFloat())
            .build()
            .toQuery()

    fun term(field: String, value: String): Query =
        Query.of { q -> q.term { t -> t.field(field).value(FieldValue.of(value)) } }

    // OR between values for a field
    fun termsAny(field: String, values: List<String>): Query {
        // Wwe want to achieve something like:
        // { "terms": { "targets": ["IOS_ios_arm64", "IOS_ios_x64"] } }

        // Wrap values into FieldValue
        val fieldValues = values.map { FieldValue.of(it) }

        // Pick the inline-list shape of `terms` (there are alternatives, i.e. lookup in another doc)
        val termsField = TermsQueryField.Builder().value(fieldValues).build()

        // The query body itself: `{"<field>": [<values>]}`.
        return TermsQuery.Builder()
            .field(field)
            .terms(termsField)
            .build()
            .toQuery()
    }

    fun bool(shoulds: List<Query>, filters: List<Query>): Query =
        Query.of { q ->
            q.bool { b ->
                // Need that or else we would have results that only fit filters, no `shoulds`
                if (shoulds.isNotEmpty()) b.should(shoulds).minimumShouldMatch("1")
                if (filters.isNotEmpty()) b.filter(filters)
                b
            }
        }

    fun scored(query: Query): Query =
        Query.of { q ->
            q.functionScore { fs ->
                fs.query(query)
                    .boostMode(FunctionBoostMode.Multiply)
                    .functions(FunctionScore.of { f ->
                        f.scriptScore { ss -> ss.script(Script.of { s -> s.inline { i -> i.source(POPULARITY_SCRIPT) } }) }
                    })
            }
        }

    fun commonFilters(
        platforms: List<PackagePlatform>,
        targetFilters: Map<TargetGroup, Set<String>>,
        ownerLogin: String?,
    ): List<Query> = buildList {
        platforms.distinct().forEach { add(term("platforms", it.name)) }
        ownerLogin?.let { add(term("owner_login.keyword", it)) }
        addAll(targetFilterClauses(targetFilters))
    }

    /** OR within a group, AND between groups. */
    private fun targetFilterClauses(targetFilters: Map<TargetGroup, Set<String>>): List<Query> = buildList {
        targetFilters.forEach { (group, targets) ->
            when (group) {
                TargetGroup.JavaScript -> add(term("platforms", "JS"))
                TargetGroup.Wasm -> add(term("platforms", "WASM"))
                TargetGroup.JVM, TargetGroup.AndroidJvm -> {
                    val start = targets.mapNotNull { group.targets.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: 0
                    add(termsAny("targets", group.targets.drop(start).map { "${group.platformName}_$it" }))
                }

                else -> when {
                    targets.isEmpty() -> add(termsAny("targets", group.targets.map { "${group.platformName}_$it" }))
                    else -> targets.forEach { add(term("targets", "${group.platformName}_$it")) }
                }
            }
        }
    }
}
