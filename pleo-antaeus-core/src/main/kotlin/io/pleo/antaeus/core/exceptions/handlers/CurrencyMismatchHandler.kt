package io.pleo.antaeus.core.exceptions.handlers

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.FailureReason.CURRENCY_MISMATCH
import io.pleo.antaeus.models.InvoiceDLQ
import mu.KotlinLogging

class CurrencyMismatchHandler: FailedPaymentHandler {
    private val log = KotlinLogging.logger {}

    override fun isResponsibleFor(): FailureReason {
        return CURRENCY_MISMATCH
    }

    override fun handle(invoiceDLQ: InvoiceDLQ) {
        //Raise an alert
        log.info { "Informing Dev team. Also have a process so that new invoices can be created with right currency: $invoiceDLQ" }
    }
}