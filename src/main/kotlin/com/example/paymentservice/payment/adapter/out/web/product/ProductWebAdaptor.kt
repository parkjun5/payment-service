package com.example.paymentservice.payment.adapter.out.web.product

import com.example.paymentservice.common.WebAdaptor
import com.example.paymentservice.payment.adapter.out.web.product.client.ProductClient
import com.example.paymentservice.payment.application.port.out.LoadProductPort
import com.example.paymentservice.payment.domain.Product
import reactor.core.publisher.Flux

@WebAdaptor
class ProductWebAdaptor (
  private val productClient: ProductClient
) : LoadProductPort {
    override fun getProducts(cartId: Long, productIds: List<Long>): Flux<Product> {
        return productClient.getProducts(cartId, productIds)
    }
}
