package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

fun interface SavePaymentPort {
    fun save(paymentEvent: PaymentEvent): Mono<Unit>
}
