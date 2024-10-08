package com.example.paymentservice.payment.adapter.out.persistent.repository

import com.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

fun interface PaymentRepository {
    fun save(paymentEvent: PaymentEvent): Mono<Unit>
}
