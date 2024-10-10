package com.example.paymentservice.payment.adapter.`in`.web.api

import com.example.paymentservice.common.WebAdaptor
import com.example.paymentservice.payment.adapter.`in`.web.request.TossPaymentConfirmRequest
import com.example.paymentservice.payment.adapter.`in`.web.response.ApiResponse
import com.example.paymentservice.payment.adapter.out.web.toss.executor.TossPaymentExecutor
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import com.example.paymentservice.payment.application.port.`in`.PaymentConfirmUseCase
import com.example.paymentservice.payment.domain.PaymentConfirmResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@WebAdaptor
@RequestMapping("/v1/toss")
@RestController
class TossPaymentController(
    private val tossPaymentExecutor: TossPaymentExecutor,
    private val paymentConfirmUseCase: PaymentConfirmUseCase
) {

    @PostMapping("confirm")
    fun confirm(@RequestBody request: TossPaymentConfirmRequest): Mono<ResponseEntity<ApiResponse<PaymentConfirmResult>>> {
        val command = PaymentConfirmCommand(
            paymentKey = request.paymentKey,
            orderId = request.orderId,
            amount = request.amount
        )

        return paymentConfirmUseCase.confirm(command)
            .map { ResponseEntity.ok().body(
                ApiResponse.with(HttpStatus.OK, "", it)
            )
        }
    }

    @PostMapping("legacy-confirm")
    fun legacyConfirm(@RequestBody request: TossPaymentConfirmRequest): Mono<ResponseEntity<ApiResponse<String>>> {
        return tossPaymentExecutor.execute(
            paymentKey = request.paymentKey,
            orderId = request.orderId,
            amount = request.amount.toString()
        ).map {
            ResponseEntity.ok().body(
                ApiResponse.with(HttpStatus.OK, "ok", it)
            )
        }
    }

}
