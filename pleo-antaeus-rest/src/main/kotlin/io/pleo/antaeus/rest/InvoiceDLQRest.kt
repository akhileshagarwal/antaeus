package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.pleo.antaeus.core.services.FailedPaymentsService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.ErrorResponse
import io.pleo.antaeus.models.FailureReason
import mu.KotlinLogging

class InvoiceDLQRest(javalin: Javalin,
                     private val invoiceService: InvoiceService,
                     private val failedPaymentsService: FailedPaymentsService) {
    private val log = KotlinLogging.logger {}

    init {
        javalin.routes {
            ApiBuilder.path("rest") {
                ApiBuilder.path("v1") {
                    ApiBuilder.path("invoices-dlq") {
                        ApiBuilder.get {
                            val reason: String? = it.queryParam("failureReason")
                            if (reason == null) {
                                it.json(invoiceService.fetchAllInvoiceDLQ())
                            } else {
                                runCatching { FailureReason.valueOf(reason) }
                                    .onSuccess { failureReason: FailureReason -> it.json(invoiceService.fetchInvoiceDLQByFailureReason(failureReason)) }
                                    .onFailure { exception: Throwable ->
                                        log.error("$reason status not found", exception)
                                        it.json(ErrorResponse("$reason status not found")).status(404)
                                    }
                            }
                        }
                        ApiBuilder.post{
                            failedPaymentsService.takeAction()
                            it.json("Executed")
                        }
                    }
                }
            }
        }
    }
}