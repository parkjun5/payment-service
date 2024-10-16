package com.example.paymentservice.payment.application.service

import com.example.paymentservice.payment.adapter.out.exception.PaymentValidationException
import com.example.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import com.example.paymentservice.payment.adapter.out.web.toss.exception.TossPaymentError
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
        assertThat(paymentEvent.isSuccess())
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
            failure = PaymentFailure("ERROR", "Test Error"),
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
        assertThat(paymentEvent.isFailure())
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
        assertThat(paymentEvent.isUnknown())
    }

    @Test
    fun `should handle PSPConfirmationException`() {
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

        val pspConfirmationException = PSPConfirmationException(
            errorCode = TossPaymentError.REJECT_ACCOUNT_PAYMENT.name,
            errorMessage = TossPaymentError.REJECT_ACCOUNT_PAYMENT.description,
            isSuccess = false,
            isFailure = true,
            isUnknown = false,
            isRetryableError = false
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.error(pspConfirmationException)

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()
        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult).isNotNull
        assertThat(paymentConfirmationResult!!.status).isEqualTo(PaymentStatus.FAILURE)
        assertThat(paymentEvent.isFailure())
    }

    @Test
    fun `should handle PaymentValidationException`() {
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

        val mockPaymentValidationPort = mockk<PaymentValidationPort>()


        val paymentConfirmService = PaymentConfirmService(
            paymentStatusUpdatePort = paymentStatusUpdatePort,
            paymentExecutorPort = mockPaymentExecutorPort,
            paymentValidationPort = mockPaymentValidationPort,
        )

        val paymentValidationException = PaymentValidationException("결제 유효성 검증에서 실패했습니다.")

        every { mockPaymentValidationPort.isValid(orderId, paymentConfirmCommand.amount ) } returns Mono.error(paymentValidationException)

        val paymentConfirmationResult = paymentConfirmService.confirm(paymentConfirmCommand).block()
        val paymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmationResult).isNotNull
        assertThat(paymentConfirmationResult!!.status).isEqualTo(PaymentStatus.FAILURE)
        assertThat(paymentEvent.isFailure())
    }
}
