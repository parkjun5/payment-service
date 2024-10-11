package com.example.paymentservice.payment.application.service

import com.example.paymentservice.common.UseCase
import com.example.paymentservice.payment.adapter.out.exception.PaymentAlreadyProcessedException
import com.example.paymentservice.payment.adapter.out.exception.PaymentValidationException
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmUseCase
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort
import com.example.paymentservice.payment.domain.PaymentConfirmResult
import com.example.paymentservice.payment.domain.PaymentFailure
import com.example.paymentservice.payment.domain.PaymentStatus
import io.netty.handler.timeout.TimeoutException
import reactor.core.publisher.Mono

@UseCase
class PaymentConfirmService (
    private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    private val paymentValidationPort: PaymentValidationPort,
    private val paymentExecutorPort: PaymentExecutorPort
) : PaymentConfirmUseCase {

    override fun confirm(command: PaymentConfirmCommand): Mono<PaymentConfirmResult> {
        return paymentStatusUpdatePort.updatePaymentStatusToExecuting(command.orderId, command.paymentKey)
            .filterWhen { paymentValidationPort.isValid(command.orderId, command.amount) }
            .flatMap { paymentExecutorPort.execute(command) }
            .flatMap { paymentStatusUpdatePort.updatePaymentStatus(
                command = PaymentStatusUpdateCommand(
                    paymentKey = it.paymentKey,
                    orderId = it.orderId,
                    status = it.paymentStatus(),
                    extraDetails = it.extraDetails,
                    failure = it.failure
                )
            ).thenReturn(it)
            }
            .map { PaymentConfirmResult(status = it.paymentStatus(), failure = it.failure) }
            .onErrorResume { error ->
                handlePaymentError(error, command)
            }
    }

    private fun handlePaymentError(
        error: Throwable,
        command: PaymentConfirmCommand
    ): Mono<PaymentConfirmResult> {
        val (status, failure) = when (error) {
            is PSPConfirmationException -> Pair(error.paymentStatus(), PaymentFailure(errorCode = error.errorCode, message = error.errorMessage))
            is PaymentValidationException -> Pair(PaymentStatus.FAILURE, PaymentFailure(errorCode = error::class.simpleName ?: "", message = error.message ?: ""))
            is PaymentAlreadyProcessedException -> return Mono.just(PaymentConfirmResult(status = error.status, failure = PaymentFailure(errorCode = error::class.simpleName ?: "", message = error.message ?: "")))
            is TimeoutException -> Pair(PaymentStatus.UNKNOWN, PaymentFailure(errorCode = error::class.simpleName ?: "", message = error.message ?: ""))
            else -> Pair(PaymentStatus.UNKNOWN, PaymentFailure(errorCode = error::class.simpleName ?: "", message = error.message ?: ""))
        }

        val paymentStatusUpdateCommand = PaymentStatusUpdateCommand(
            paymentKey = command.paymentKey,
            orderId = command.orderId,
            status = status,
            failure = failure
        )
        return paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand)
            .map { PaymentConfirmResult(status = status, failure = failure) }
    }

}
