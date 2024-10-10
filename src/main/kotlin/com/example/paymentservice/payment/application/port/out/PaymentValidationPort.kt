package com.example.paymentservice.payment.application.port.out

import reactor.core.publisher.Mono

fun interface PaymentValidationPort {
    fun isValid(orderId: String, amount: Long): Mono<Boolean>
}
