package com.example.paymentservice.payment.adapter.`in`.web.view

import com.example.paymentservice.common.WebAdaptor
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
@WebAdaptor
class CheckoutController {

    @GetMapping("/")
    fun checkoutPage(): Mono<String> {
        return Mono.just("checkout")
    }

}
