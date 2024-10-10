package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.domain.PaymentExecutionResult
import reactor.core.publisher.Mono

fun interface PaymentExecutorPort {
    fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult>
}
