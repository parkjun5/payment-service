package com.example.paymentservice.common

import java.util.UUID

object IdempotencyCreator {
    fun create(date: Any): String {
        return UUID.nameUUIDFromBytes(date.toString().toByteArray()).toString()
    }
}
