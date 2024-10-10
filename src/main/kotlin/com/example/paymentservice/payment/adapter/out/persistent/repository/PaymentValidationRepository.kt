package com.example.paymentservice.payment.adapter.out.persistent.repository

import reactor.core.publisher.Mono

fun interface PaymentValidationRepository {
    fun isValid(orderId: String, amount: Long): Mono<Boolean>
}
