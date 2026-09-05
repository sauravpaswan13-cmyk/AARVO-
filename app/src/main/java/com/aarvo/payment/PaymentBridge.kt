package com.aarvo.payment

import com.razorpay.PaymentData

/** Holds the short-lived Razorpay callback values while the UI verifies them on the server. */
object PaymentBridge {
    @Volatile var lastSignature: String? = null
    @Volatile var lastOrderId: String? = null

    fun capture(paymentData: PaymentData?) {
        lastSignature = paymentData?.signature
        lastOrderId = paymentData?.orderId
    }

    fun clear() {
        lastSignature = null
        lastOrderId = null
    }
}
