package io.pleo.antaeus.core.exceptions.handlers

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.FailureReason.INSUFFICIENT_FUNDS
import io.pleo.antaeus.models.InvoiceDLQ
import mu.KotlinLogging

class InSufficientFundsHandler: FailedPaymentHandler {
    private val log = KotlinLogging.logger {}

    override fun isResponsibleFor(): FailureReason {
        return INSUFFICIENT_FUNDS
    }

    override fun handle(invoiceDLQ: InvoiceDLQ) {
        //In this case start the dunning on the customer and create a new Invoice
        // so that it can be picked up again in next month cycle
        // Also inform the customer about this
        log.info { "Due to insufficient funds, informing Dunning System to start the dunning " }
        log.info { "Create a new Invoice so that it can be picked up again in next month cycle for : $invoiceDLQ" }
    }
}