package io.klibs.core.user.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class ExternalUserIdentityTest {

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t", "\n"])
    fun `rejects a blank external user id`(externalUserId: String) {
        assertFailsWith<IllegalArgumentException> {
            ExternalUserIdentity(externalUserId)
        }
    }
}
