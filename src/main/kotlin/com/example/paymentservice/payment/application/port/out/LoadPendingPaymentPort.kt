package com.example.paymentservice.payment.application.port.out

import com.example.paymentservice.payment.domain.PendingPaymentEvent
import reactor.core.publisher.Flux

fun interface LoadPendingPaymentPort {

    fun getPendingPayments(): Flux<PendingPaymentEvent>
}
