package io.pleo.antaeus.core.exceptions.handlers

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.InvoiceDLQ

interface FailedPaymentHandler {
    fun isResponsibleFor(): FailureReason
    fun handle(invoiceDLQ: InvoiceDLQ)
}