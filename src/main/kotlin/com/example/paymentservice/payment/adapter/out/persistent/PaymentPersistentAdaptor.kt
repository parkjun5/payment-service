package com.example.paymentservice.payment.adapter.out.persistent

import com.example.paymentservice.common.PersistentAdaptor
import com.example.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import com.example.paymentservice.payment.application.port.out.SavePaymentPort
import com.example.paymentservice.payment.domain.PaymentEvent
import reactor.core.publisher.Mono

@PersistentAdaptor
class PaymentPersistentAdaptor (
    private val paymentRepository: PaymentRepository
) : SavePaymentPort {

    override fun save(paymentEvent: PaymentEvent): Mono<Unit> {
        return paymentRepository.save(paymentEvent)
    }
}
