package com.example.paymentservice.payment.application.port.`in`

import com.example.paymentservice.payment.domain.PaymentConfirmResult
import reactor.core.publisher.Mono

fun interface PaymentConfirmUseCase {
    fun confirm(command: PaymentConfirmCommand): Mono<PaymentConfirmResult>
}
