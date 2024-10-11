package com.example.paymentservice.payment.test

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.util.*

@TestConfiguration
class PSPTestWebClientConfiguration (
    @Value("\${PSP.toss.url}") private val baseUrl: String,
    @Value("\${PSP.toss.secretKey}") private val secretKey: String,
) {

    fun createTestTossWebClient(vararg customHeaderKeyValue: Pair<String, String>): WebClient {
        val encodeSecretKey = Base64.getEncoder().encodeToString(("$secretKey:").toByteArray())

        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $encodeSecretKey")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeaders { httpHeaders ->
                customHeaderKeyValue.forEach { httpHeaders[it.first] =  it.second }
            }
            .clientConnector(reactorClientHttpConnector())
            .build()
    }

    private fun reactorClientHttpConnector(): ClientHttpConnector {
        val connectionProvider = ConnectionProvider.builder("test-toss-payment").build()
        return ReactorClientHttpConnector(HttpClient.create(connectionProvider))
    }
}
