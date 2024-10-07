package com.example.paymentservice.payment.adapter.out.web.executor

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class TossPaymentExecutor(
    private val tossPaymentWebClient: WebClient,
) {

    fun execute(paymentKey: String, orderId: String, amount: String): Mono<String> {
        return tossPaymentWebClient.post()
            .uri(URL)
            .bodyValue("""
                {
                    "paymentKey":  ${paymentKey},
                    "orderId":  ${orderId},
                    "amount":  ${amount},
            """.trimIndent())
            .retrieve()
            .bodyToMono(String::class.java)
    }

    companion object {
        private const val URL: String = "/v1/payments/confirm"
    }
}
