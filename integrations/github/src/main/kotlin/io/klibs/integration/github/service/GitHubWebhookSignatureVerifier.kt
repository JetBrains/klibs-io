package io.klibs.integration.github.service

interface GitHubWebhookSignatureVerifier {
    fun isValid(payload: ByteArray, signatureHeader: String?): Boolean
}
