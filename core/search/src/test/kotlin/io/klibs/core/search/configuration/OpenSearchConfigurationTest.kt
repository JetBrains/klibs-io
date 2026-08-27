package io.klibs.core.search.configuration

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.boot.ssl.DefaultSslBundleRegistry
import org.springframework.boot.ssl.SslBundle
import org.springframework.boot.ssl.SslBundles
import org.springframework.boot.ssl.pem.PemSslStoreBundle
import org.springframework.boot.ssl.pem.PemSslStoreDetails
import javax.net.ssl.SSLContext

class OpenSearchConfigurationTest {

    private val configuration = OpenSearchConfiguration()
    private val mapper = configuration.openSearchJsonpMapper()

    @Test
    fun `client is built with the default JVM trust store`() {
        val client = configuration.openSearchClient(OpenSearchProperties(), mapper, DefaultSslBundleRegistry())

        assertNotNull(client)
    }

    @Test
    fun `client is built when all certificates are trusted`() {
        val properties = OpenSearchProperties(trustAllCertificates = true)

        val client = configuration.openSearchClient(properties, mapper, DefaultSslBundleRegistry())

        assertNotNull(client)
    }

    @Test
    fun `building client from the opensearch ssl bundle does not replace JVM default ssl context`() {
        val bundles: SslBundles = DefaultSslBundleRegistry("opensearch", pemTrustBundle(SELF_SIGNED_CERTIFICATE))
        val previousDefault = SSLContext.getDefault()
        try {
            val client = configuration.openSearchClient(OpenSearchProperties(), mapper, bundles)

            assertNotNull(client)
            assertSame(previousDefault, SSLContext.getDefault())
        } finally {
            SSLContext.setDefault(previousDefault)
        }
    }

    private fun pemTrustBundle(certificate: String): SslBundle {
        val stores = PemSslStoreBundle(null, PemSslStoreDetails.forCertificate(certificate))
        return SslBundle.of(stores)
    }

    private companion object {
        val SELF_SIGNED_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIDCTCCAfGgAwIBAgIUZA0Q2T2pjdtbIWI2nqfRJm5vu+YwDQYJKoZIhvcNAQEL
            BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDgxMzA3NTcxMVoXDTM2MDgx
            MDA3NTcxMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
            AAOCAQ8AMIIBCgKCAQEAw+JTXQ9no8zdRwRHcbbfbqizgZPxadvYC+Ubb9Fu20mO
            uUTE0JpayTMj9Avf8Q31EjxX8OBkeITKmENlfMaQUxae6EtIYbVwGr9bsyx++jg0
            5K6JKTGMdHH/CzeTu+WTCdxQPXEOVjIAsNey135zqEFc8e+JsYQIjUF+g5saIUt7
            ybgtEmvul2movgDCGNqq1gpHqJdDpjzIx2nqBMpsTXB7DJONkRjizU46j7Wz4aRp
            t9QZXp01bBWYIKcfIimeOvDRmt+up+lxjxbrL+JGcNes3cci1wh+Xb4oPt3Pc8mG
            y17HydG2bojLwA8upA96B1NxuhR8PjBrL1iCSWmv0wIDAQABo1MwUTAdBgNVHQ4E
            FgQUc6C4vJLbHca5PTJZy1tUno7+Pi8wHwYDVR0jBBgwFoAUc6C4vJLbHca5PTJZ
            y1tUno7+Pi8wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEADyOT
            Eg/mxXExTMvT/bDnvXrAnDsnLCmekfRiAlz6ddLTmr1Ifs/6L7H1ZHzsJvk/+TJb
            teb9xZMy+EnKQ1BpsyNUp/rwCaNLsxeuQZ0B6/YDLKc4IZhvtke9+K+7K10H5AAT
            voqjVUyhm17fDNF0bGE5lhR+DPVU6TtfYtH79fPXfx/sT8PrAH1Lj6tq87H3Jz+C
            j3RYBK8kYvKE7oF6TbF3ogbcuyvknLUZTWso8dsd1BXQAy9BqqfOYdoXEcUkUEh7
            rXwJc90SPIkGD1riWbRIWvfWepAYsX55wbBF4C2L/XgxhFDsiDUGb26cE9zyhYOJ
            sCaI3ol+ET7A7/3lOw==
            -----END CERTIFICATE-----
        """.trimIndent()
    }
}
