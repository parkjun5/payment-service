package com.example.paymentservice.payment.adapter.out.web.toss.response

data class TossPaymentConfirmationResponse(
    val version: String,
    val paymentKey: String,
    val type: String,
    val orderId: String,
    val orderName: String,
    val mId: String,
    val currency: String,
    val method: String,
    val totalAmount: Long,
    val balanceAmount: Long,
    val status: String,
    val requestedAt: String,
    val approvedAt: String,
    val useEscrow: Boolean,
    val lastTransactionKey: String?,
    val suppliedAmount: Long,
    val vat: Long,
    val cultureExpense: Boolean,
    val taxFreeAmount: Long,
    val taxExemptionAmount: Int,
    val cancels: List<Cancel>?,
    val isPartialCancelable: Boolean,
    val card: Card?,
    val virtualAccount: VirtualAccount?,
    val mobilePhone: MobilePhone?,
    val giftCertificate: GiftCertificate?,
    val transfer: Transfer?,
    val receipt: Receipt?,
    val checkout: Checkout?,
    val easyPay: EasyPay?,
    val country: String,
    val failure: Failure?,
    val cashReceipt: CashReceipt?,
    val cashReceipts: List<CashReceiptDetail>?,
    val discount: Discount?
)

data class Cancel(
    val cancelAmount: Long,
    val cancelReason: String,
    val taxFreeAmount: Long,
    val taxExemptionAmount: Int,
    val refundableAmount: Long,
    val easyPayDiscountAmount: Long,
    val canceledAt: String,
    val transactionKey: String,
    val receiptKey: String?,
    val cancelStatus: String
)

data class Card(
    val amount: Long,
    val issuerCode: String,
    val acquirerCode: String?,
    val number: String,
    val installmentPlanMonths: Int,
    val approveNo: String,
    val useCardPoint: Boolean,
    val cardType: String,
    val ownerType: String,
    val acquireStatus: String,
    val isInterestFree: Boolean,
    val interestPayer: String?
)

data class VirtualAccount(
    val accountType: String,
    val accountNumber: String,
    val bankCode: String,
    val customerName: String,
    val dueDate: String,
    val refundStatus: String,
    val expired: Boolean,
    val settlementStatus: String,
    val refundReceiveAccount: RefundReceiveAccount?,
    val secret: String?
)

data class RefundReceiveAccount(
    val bankCode: String,
    val accountNumber: String,
    val holderName: String
)

data class MobilePhone(
    val customerMobilePhone: String,
    val settlementStatus: String,
    val receiptUrl: String
)

data class GiftCertificate(
    val approveNo: String,
    val settlementStatus: String
)

data class Transfer(
    val bankCode: String,
    val settlementStatus: String
)

data class Receipt(
    val url: String
)

data class Checkout(
    val url: String
)

data class EasyPay(
    val provider: String,
    val amount: Long,
    val discountAmount: Long
)

data class Failure(
    val code: String,
    val message: String
)

data class CashReceipt(
    val type: String,
    val receiptKey: String,
    val issueNumber: String,
    val receiptUrl: String,
    val amount: Long,
    val taxFreeAmount: Long
)

data class CashReceiptDetail(
    val receiptKey: String,
    val orderId: String,
    val orderName: String,
    val type: String,
    val issueNumber: String,
    val receiptUrl: String,
    val businessNumber: String,
    val transactionType: String,
    val amount: Int,
    val taxFreeAmount: Int,
    val issueStatus: String,
    val failure: Failure?,
    val customerIdentityNumber: String,
    val requestedAt: String
)

data class Discount(
    val amount: Int
)
