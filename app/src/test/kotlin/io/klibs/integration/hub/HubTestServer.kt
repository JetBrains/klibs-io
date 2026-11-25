package io.klibs.integration.hub

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class HubTestServer : AutoCloseable {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        this.executor = this@HubTestServer.executor
        createContext("/hub/api/rest/oauth2/", ::handle)
        start()
    }
    val baseUrl: String = "http://127.0.0.1:${server.address.port}/hub"
    val requests = LinkedBlockingQueue<HubRequestDto>()

    @Volatile var tokenResponse = 200 to """{"access_token":"hub-access-token","token_type":"Bearer"}"""
    @Volatile var userInfoResponse = 200 to """{"sub":"hub-user-42"}"""
    @Volatile var tokenBodyGate: CountDownLatch? = null
    @Volatile var userInfoBodyGate: CountDownLatch? = null

    fun reset() {
        tokenBodyGate?.countDown()
        userInfoBodyGate?.countDown()
        tokenBodyGate = null
        userInfoBodyGate = null
        tokenResponse = 200 to """{"access_token":"hub-access-token","token_type":"Bearer"}"""
        userInfoResponse = 200 to """{"sub":"hub-user-42"}"""
        requests.clear()
    }

    override fun close() {
        tokenBodyGate?.countDown()
        userInfoBodyGate?.countDown()
        server.stop(0)
        executor.shutdownNow()
    }

    private fun handle(exchange: HttpExchange) {
        exchange.use {
            requests.add(
                HubRequestDto(
                    exchange.requestMethod,
                    exchange.requestURI.path,
                    exchange.requestHeaders.getFirst("Authorization"),
                    exchange.requestHeaders.getFirst("Content-Type"),
                    exchange.requestBody.bufferedReader().readText(),
                ),
            )
            val isTokenRequest = exchange.requestURI.path.endsWith("/token")
            val (status, body) = if (isTokenRequest) tokenResponse else userInfoResponse
            val bodyGate = if (isTokenRequest) tokenBodyGate else userInfoBodyGate
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            if (status in 300..399) {
                exchange.responseHeaders.add("Location", "$baseUrl/api/rest/oauth2/redirected")
            }
            try {
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.flush()
                bodyGate?.await(5, TimeUnit.SECONDS)
                exchange.responseBody.write(bytes)
            } catch (_: IOException) {
                // Expected when a timeout cancels the client while this fixture is holding the response body.
            }
        }
    }
}
