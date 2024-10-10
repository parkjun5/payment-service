package com.example.paymentservice.payment.application.service

import com.example.paymentservice.payment.application.port.`in`.CheckoutCommand
import com.example.paymentservice.payment.application.port.`in`.CheckoutUseCase
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.application.port.out.PaymentExecutorPort
import com.example.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import com.example.paymentservice.payment.application.port.out.PaymentValidationPort
import com.example.paymentservice.payment.domain.*
import com.example.paymentservice.payment.test.PaymentDatabaseHelper
import com.example.paymentservice.payment.test.PaymentTestConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.Test

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class PaymentConfirmServiceTest(
    @Autowired private val checkoutUseCase: CheckoutUseCase,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper
) {

    private val mockPaymentExecutorPort = mockk<PaymentExecutorPort>()

    @BeforeEach
    fun setUp() {
        paymentDatabaseHelper.clear().block()
    }

    @Test
    fun `should be marked as SUCESS if Payment Confirmation success in PSP`() {
        Hooks.onOperatorDebug()
        mockkConstructor(PaymentConfirmService::class)

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = checkoutResult.amount,
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = checkoutResult.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            isSuccess = true,
            isFailure = false,
            isUnknown = false,
            isRetryable = false
        )
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) }
            .returns(Mono.just(paymentExecutionResult))

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult).isNotNull
        assertThat(paymentConfirmationResult!!.status).isEqualTo(PaymentStatus.SUCCESS)
        assertThat(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.SUCCESS })
        assertThat(paymentEvent.paymentType).isEqualTo(paymentExecutionResult.extraDetails!!.type)
        assertThat(paymentEvent.paymentMethod).isEqualTo(paymentExecutionResult.extraDetails!!.method)
        assertThat(paymentEvent.orderName).isEqualTo(paymentExecutionResult.extraDetails!!.orderName)
        assertThat(paymentEvent.approvedAt?.truncatedTo(
            ChronoUnit.MINUTES
        )).isEqualTo(
            paymentExecutionResult.extraDetails!!.approvedAt.truncatedTo(
                ChronoUnit.MINUTES
            )
        )
    }

    @Test
    fun `should be marked as FAILURE if Payment Confirmation fails in PSP`() {
        Hooks.onOperatorDebug()
        mockkConstructor(PaymentConfirmService::class)

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = checkoutResult.amount,
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = checkoutResult.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            failure = PaymentExecutionFailure("ERROR", "Test Error"),
            isSuccess = false,
            isFailure = true,
            isUnknown = false,
            isRetryable = false
        )
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) }
            .returns(Mono.just(paymentExecutionResult))

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult).isNotNull
        assertThat(paymentConfirmationResult!!.status).isEqualTo(PaymentStatus.FAILURE)
        assertThat(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.FAILURE })
    }

    @Test
    fun `should be marked as UNKNOWN if Payment Confirmation fails due to unknown exception`() {
        Hooks.onOperatorDebug()
        mockkConstructor(PaymentConfirmService::class)

        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = checkoutResult.amount,
        )

        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentValidationPort = paymentValidationPort,
            paymentExecutorPort = mockPaymentExecutorPort
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                totalAmount = checkoutResult.amount,
                orderName = "test_order_name",
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                approvedAt = LocalDateTime.now(),
                pspRawData = "{}"
            ),
            isSuccess = false,
            isFailure = false,
            isUnknown = true,
            isRetryable = false
        )
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) }
            .returns(Mono.just(paymentExecutionResult))

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()

        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult).isNotNull
        assertThat(paymentConfirmationResult!!.status).isEqualTo(PaymentStatus.UNKNOWN)
        assertThat(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.UNKNOWN })
    }
}
