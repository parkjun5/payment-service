package com.example.paymentservice.payment.application.port.`in`

import com.example.paymentservice.payment.domain.CheckoutResult
import reactor.core.publisher.Mono

fun interface CheckoutUseCase {
    fun checkout(command: CheckoutCommand): Mono<CheckoutResult>
}
