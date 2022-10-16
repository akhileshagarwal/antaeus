package io.pleo.antaeus.core.exceptions.handlers

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.FailureReason.*
import io.pleo.antaeus.models.InvoiceDLQ
import mu.KotlinLogging

class NetworkFailureHandler: FailedPaymentHandler {
    private val log = KotlinLogging.logger {}
    override fun isResponsibleFor(): FailureReason {
        return NETWORK_FAILURE
    }

    override fun handle(invoiceDLQ: InvoiceDLQ) {
        log.info { "Inform the dev team about Network failure for this PSP and raise an alert(Datadog/Graphana/Sentri are some options): $invoiceDLQ" }
    }
}