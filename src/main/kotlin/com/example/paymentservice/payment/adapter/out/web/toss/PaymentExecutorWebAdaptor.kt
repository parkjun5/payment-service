package com.example.paymentservice.payment.adapter.out.web.toss

import com.example.paymentservice.common.WebAdaptor
import com.example.paymentservice.payment.adapter.out.web.toss.executor.PaymentExecutor
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort
import com.example.paymentservice.payment.domain.PaymentExecutionResult
import reactor.core.publisher.Mono

@WebAdaptor
class PaymentExecutorWebAdaptor (
    private val paymentExecutor: PaymentExecutor,
) : PaymentExecutorPort {

    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return paymentExecutor.execute(command)
    }

}
