package io.pleo.antaeus.core.exceptions.handlers

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.FailureReason.*
import io.pleo.antaeus.models.InvoiceDLQ
import mu.KotlinLogging

class CustomerNotFoundHandler: FailedPaymentHandler {
    private val log = KotlinLogging.logger {}

    override fun isResponsibleFor(): FailureReason {
        return CUSTOMER_NOT_FOUND
    }

    override fun handle(invoiceDLQ: InvoiceDLQ) {
        // Raise an alert and call the circuit breaker for this customer. Also inform the customer about this
        log.info { "Informing Dev team about Customer Not Found Exception for: $invoiceDLQ" }
    }
}