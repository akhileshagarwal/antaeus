package io.pleo.antaeus.models

data class InvoiceDLQ(
    val id: Int,
    val invoiceId: Int,
    val failureReason: FailureReason,
    var isHandled: Boolean
)