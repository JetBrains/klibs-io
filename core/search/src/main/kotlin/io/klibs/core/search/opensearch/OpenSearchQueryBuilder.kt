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

    /** Search-time analyzer declared in settings.json; emits alias tokens only. */
    private const val TOOL_ALIAS_ANALYZER = "tool_alias"

    /** Partial-match subfields declared in project-mappings.json: token prefixes and token tails. */
    private const val PREFIX_SUBFIELD = "prefix"
    private const val SUFFIX_SUBFIELD = "suffix"

    /** English-stemmed and delimiter-split subfields declared in project-mappings.json. */
    const val ENGLISH_SUBFIELD = "en"
    const val SPLIT_SUBFIELD = "split"

    /** Query lengths a partial clause can serve; mirrors `min_gram`/`max_gram` in settings.json. */
    const val MIN_PARTIAL_LENGTH = 3
    const val MAX_PARTIAL_LENGTH = 18

    // Multiplier for the bm25 score for each matching item.
    // - Score is higher if readme is present.
    // - Each popularity signal is passed through a saturation curve `x / (x + pivot)`: diminishing
    //   like log, but bounded in [0,1), so a weight below is that signal's real maximum share.
    // - A pivot scores half its weight and places the range the curve can still tell apart — most
    //   resolution below it, flat well above. log spent its own on 5 vs 50 stars.
    private const val STARS_PIVOT = 300.0
    private const val DEPENDENTS_PIVOT = 3.0

    private const val STARS_WEIGHT = 3.0
    private const val DEPENDENTS_WEIGHT = 4.0

    private const val POPULARITY_SCRIPT =
        "double d = doc['${ProjectFields.HAS_README}'].value ? 1.0 : 0.7; " +
                "double s = doc['${ProjectFields.STARS}'].value; " +
                "double p = doc['${ProjectFields.DEPENDENT_COUNT}'].value; " +
                "return 1 + (" +
                "$STARS_WEIGHT * (s / (s + $STARS_PIVOT)) + " +
                "$DEPENDENTS_WEIGHT * (p / (p + $DEPENDENTS_PIVOT))" +
                ") * d;"

    // Word-bag match: OR over the query terms, order-insensitive, and a doc matching only some of
    // them still scores — query "ktor client" matches "ktor-server", just with a low score.
    // `text` goes in as a FieldValue, so the serializer quotes it — never concatenated into the DSL.
    fun match(field: String, text: String, boost: Int): Query =
        MatchQuery.Builder()
            .field(field)
            .query(FieldValue.of(text))
            .boost(boost.toFloat())
            .build()
            .toQuery()

    // Curated tool aliases from settings.json: "Hilt" -> koin/kodein, "Room alternative" -> sqldelight.
    // The `tool_alias` analyzer drops everything except SYNONYM tokens, so a query naming no tool
    // produces no terms at all and this clause contributes nothing — plain queries rank as before.
    fun toolAlias(field: String, text: String, boost: Int): Query =
        MatchQuery.Builder()
            .field(field)
            .query(FieldValue.of(text))
            .analyzer(TOOL_ALIAS_ANALYZER)
            .boost(boost.toFloat())
            .build()
            .toQuery()

    fun tokenPrefix(field: String, text: String, boost: Float): Query = partial(PREFIX_SUBFIELD, field, text, boost)

    fun tokenSuffix(field: String, text: String, boost: Float): Query = partial(SUFFIX_SUBFIELD, field, text, boost)

    /** The English-stemmed form of [field]: `charts` and `charting` both reachable from `chart`. */
    fun english(field: String, text: String, boost: Float): Query = partial(ENGLISH_SUBFIELD, field, text, boost)

    /** The delimiter- and camelCase-split form of [field]: `ComposeCharts` -> `compose` + `charts`. */
    fun split(field: String, text: String, boost: Float): Query = partial(SPLIT_SUBFIELD, field, text, boost)

    private fun partial(subfield: String, field: String, text: String, boost: Float): Query =
        MatchQuery.Builder()
            .field("$field.$subfield")
            .query(FieldValue.of(text))
            .boost(boost)
            .build()
            .toQuery()

    fun fuzzy(field: String, text: String, boost: Int): Query =
        MatchQuery.Builder()
            .field(field)
            .query(FieldValue.of(text))
            // scales the allowed-edits to term length
            .fuzziness("AUTO")
            .prefixLength(1)
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
        // We want to achieve something like:
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

    // Best-of instead of sum-of: useful if working with alternative readings of the same evidence
    fun bestOf(alternatives: List<Query>, tieBreaker: Float): Query =
        alternatives.singleOrNull() ?: Query.of { q ->
            q.disMax { d -> d.queries(alternatives).tieBreaker(tieBreaker) }
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
        targetGroupFilters: List<Map<TargetGroup, Set<String>>>,
        ownerLogin: String?,
    ): List<Query> = buildList {
        platforms.distinct().forEach { add(term(ProjectFields.PLATFORMS, it.name)) }
        ownerLogin?.let { add(term(ProjectFields.OWNER_LOGIN.keyword, it)) }
        addAll(targetGroupFiltersClauses(targetGroupFilters))
    }

    /** OR within a map (group set), AND between maps. */
    private fun targetGroupFiltersClauses(targetGroupFilters: List<Map<TargetGroup, Set<String>>>): List<Query> = buildList {
        targetGroupFilters.forEach { orGroup ->
            val groupQueries = orGroup.mapNotNull { (group, targets) -> createOrQueryWithinOneTargetGroup(group, targets) }
            when {
                groupQueries.isEmpty() -> {}
                groupQueries.size == 1 -> add(groupQueries.single())
                else -> add(bool(shoulds = groupQueries, filters = emptyList()))
            }
        }
    }

    private fun createOrQueryWithinOneTargetGroup(targetGroup: TargetGroup, targets: Set<String>): Query? = when (targetGroup) {
        TargetGroup.JavaScript -> term(ProjectFields.PLATFORMS, "JS")
        TargetGroup.Wasm -> term(ProjectFields.PLATFORMS, "WASM")
        TargetGroup.JVM, TargetGroup.AndroidJvm -> {
            val start = targets.mapNotNull { targetGroup.targets.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: 0
            termsAny(ProjectFields.TARGETS, targetGroup.targets.drop(start).map { "${targetGroup.platform}_$it" })
        }
        TargetGroup.Unknown -> {
            null
        }

        else -> when {
            targets.isEmpty() -> termsAny(ProjectFields.TARGETS, targetGroup.targets.map { "${targetGroup.platform}_$it" })
            targets.size == 1 -> term(ProjectFields.TARGETS, "${targetGroup.platform}_${targets.single()}")
            else -> bool(
                shoulds = emptyList(),
                filters = targets.map { term(ProjectFields.TARGETS, "${targetGroup.platform}_$it") },
            )
        }
    }
}
