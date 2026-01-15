package io.klibs.integration.maven.search.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.klibs.integration.maven.MavenIntegrationProperties
import io.klibs.integration.maven.request.impl.MavenCentralRateLimiter
import org.apache.maven.search.api.MAVEN
import org.apache.maven.search.api.Record
import org.apache.maven.search.api.SearchRequest
import org.apache.maven.search.api.request.BooleanQuery
import org.apache.maven.search.api.request.Field
import org.apache.maven.search.api.request.FieldQuery
import org.apache.maven.search.api.request.Query
import org.apache.maven.search.api.transport.Transport
import org.apache.maven.search.backend.smo.SmoSearchBackend
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.DEFAULT_BACKEND_ID
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory.DEFAULT_REPOSITORY_ID
import org.apache.maven.search.backend.smo.SmoSearchResponse
import org.apache.maven.search.backend.smo.internal.SmoSearchBackendImpl
import org.apache.maven.search.backend.smo.internal.SmoSearchResponseImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.Map

@Component("CENTRAL_SONATYPE")
class CentralSonatypeSearchClient(
    properties: MavenIntegrationProperties,
    mavenCentralRateLimiter: MavenCentralRateLimiter,
    objectMapper: ObjectMapper
) : BaseMavenSearchClient(
    properties.central.searchEndpoint,
    mavenCentralRateLimiter,
    LoggerFactory.getLogger(CentralSonatypeSearchClient::class.java),
    objectMapper
) {
    override fun createSearchBackend(): SmoSearchBackend {
        return CustomSmoSearchBackendImpl(
            DEFAULT_BACKEND_ID,
            DEFAULT_REPOSITORY_ID,
            "$baseUrl/solrsearch/select",
            clientTransport
        )
    }
}

/*
 * Copyright 2025 Apache [maven-indexer].
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Dmitrii Krasnov, 2025.
 * The original code was written in Java and has been adapted for Kotlin,
 * with modifications to the `toURI` function.
 */
// TODO(Dmitrii Krasnov): remove this class, when `toURI` method will be updated in original library.
private class CustomSmoSearchBackendImpl(
    backendId: String,
    repositoryId: String,
    smoUri: String,
    transport: Transport
) : SmoSearchBackendImpl(backendId, repositoryId, smoUri, transport) {

    private val smoUri: String

    private val transport: Transport

    private val commonHeaders: MutableMap<String?, String?>

    private fun discoverVersion(): String? {
        val properties = Properties()
        val inputStream = javaClass
            .getClassLoader()
            .getResourceAsStream("org/apache/maven/search/backend/smo/internal/smo-version.properties")
        if (inputStream != null) {
            try {
                inputStream.use { `is` ->
                    properties.load(`is`)
                }
            } catch (e: IOException) {
            }
        }
        return properties.getProperty("version", "unknown")
    }

    override fun getSmoUri(): String {
        return smoUri
    }

    @Throws(IOException::class)
    override fun search(searchRequest: SearchRequest): SmoSearchResponse {
        val searchUri = toURI(searchRequest)
        val payload = fetch(searchUri, commonHeaders)
        val raw = JsonParser.parseString(payload).getAsJsonObject()
        val page: MutableList<Record?> = ArrayList<Record?>(searchRequest.paging.pageSize)
        val totalHits = populateFromRaw(raw, page)
        return SmoSearchResponseImpl(searchRequest, totalHits, page, searchUri, payload)
    }

    private fun toURI(searchRequest: SearchRequest): String {
        val paging = searchRequest.paging
        val searchedFields = HashSet<Field?>()
        var smoQuery = toSMOQuery(searchedFields, searchRequest.query)
        smoQuery += "&start=" + paging.pageOffset
        smoQuery += "&rows=" + paging.pageSize
        smoQuery += "&wt=json"
        if (searchedFields.contains(MAVEN.GROUP_ID) && searchedFields.contains(MAVEN.ARTIFACT_ID)) {
            smoQuery += "&core=gav"
        }
        return smoUri + "?q=" + smoQuery
    }

    @Throws(IOException::class)
    private fun fetch(serviceUri: String?, headers: MutableMap<String?, String?>?): String {
        transport.get(serviceUri, headers).use { response ->
            if (response.code == HttpURLConnection.HTTP_OK) {
                return String(response.body.readAllBytes(), StandardCharsets.UTF_8)
            } else {
                throw IOException("Unexpected response: " + response)
            }
        }
    }

    private fun toSMOQuery(searchedFields: HashSet<Field?>, query: Query): String {
        if (query is BooleanQuery.And) {
            val bq = query as BooleanQuery
            return toSMOQuery(searchedFields, bq.left) + "%20AND%20" + toSMOQuery(searchedFields, bq.right)
        } else if (query is FieldQuery) {
            val fq = query
            val smoFieldName: String? = FIELD_TRANSLATION.get(fq.field)
            if (smoFieldName != null) {
                searchedFields.add(fq.field)
                return smoFieldName + ":" + encodeQueryParameterValue(fq.value)
            } else {
                throw IllegalArgumentException("Unsupported SMO field: " + fq.field)
            }
        }
        return encodeQueryParameterValue(query.value)
    }

    private fun encodeQueryParameterValue(parameterValue: String): String {
        return URLEncoder.encode(parameterValue, StandardCharsets.UTF_8).replace("+", "%20")
    }

    private fun populateFromRaw(raw: JsonObject, page: MutableList<Record?>): Int {
        val response = raw.getAsJsonObject("response")
        val numFound = response.get("numFound").asNumber

        val docs = response.getAsJsonArray("docs")
        for (doc in docs) {
            page.add(convert((doc as JsonObject?)!!))
        }
        return numFound.toInt()
    }

    private fun convert(doc: JsonObject): Record {
        val result = HashMap<Field?, Any?>()

        mayPut(result, MAVEN.GROUP_ID, mayGet("g", doc))
        mayPut(
            result,
            MAVEN.ARTIFACT_ID,
            mayGet("a", doc)
        )
        var version: String? = mayGet("v", doc)
        if (version == null) {
            version = mayGet("latestVersion", doc)
        }
        mayPut(result, MAVEN.VERSION, version)
        mayPut(result, MAVEN.PACKAGING, mayGet("p", doc))
        mayPut(result, MAVEN.CLASSIFIER, mayGet("l", doc))

        // version count
        val versionCount = if (doc.has("versionCount")) doc.get("versionCount").asNumber else null
        if (versionCount != null) {
            mayPut(result, MAVEN.VERSION_COUNT, versionCount.toInt())
        }
        // ec
        val ec = doc.getAsJsonArray("ec")
        if (ec != null) {
            result.put(MAVEN.HAS_SOURCE, ec.contains(EC_SOURCE_JAR))
            result.put(MAVEN.HAS_JAVADOC, ec.contains(EC_JAVADOC_JAR))
        }

        return Record(
            backendId,
            repositoryId,
            if (doc.has("id")) doc.get("id").asString else null,
            if (doc.has("timestamp")) doc.get("timestamp").asLong else null,
            result
        )
    }

    /**
     * Creates a customized instance of SMO backend, like an in-house instances of SMO or different IDs.
     */
    init {
        this.smoUri = Objects.requireNonNull<String?>(smoUri)
        this.transport = Objects.requireNonNull<Transport?>(transport)

        this.commonHeaders = HashMap<String?, String?>()
        this.commonHeaders.put(
            "User-Agent",
            ("Apache-Maven-Search-SMO/" + discoverVersion() + " "
                    + transport.javaClass.getSimpleName())
        )
        this.commonHeaders.put("Accept", "application/json")
    }

    companion object {
        private val FIELD_TRANSLATION: MutableMap<Field, String> = Map.of<Field, String>(
            MAVEN.GROUP_ID,
            "g",
            MAVEN.ARTIFACT_ID,
            "a",
            MAVEN.VERSION,
            "v",
            MAVEN.CLASSIFIER,
            "l",
            MAVEN.PACKAGING,
            "p",
            MAVEN.CLASS_NAME,
            "c",
            MAVEN.FQ_CLASS_NAME,
            "fc",
            MAVEN.SHA1,
            "1"
        )

        private val EC_SOURCE_JAR = JsonPrimitive("-sources.jar")

        private val EC_JAVADOC_JAR = JsonPrimitive("-javadoc.jar")

        private fun mayGet(field: String?, `object`: JsonObject): String? {
            return if (`object`.has(field)) `object`.get(field).asString else null
        }

        private fun mayPut(result: MutableMap<Field?, Any?>, fieldName: Field?, value: Any?) {
            if (value == null) {
                return
            }
            if (value is String && value.trim { it <= ' ' }.isEmpty()) {
                return
            }
            result.put(fieldName, value)
        }
    }
}