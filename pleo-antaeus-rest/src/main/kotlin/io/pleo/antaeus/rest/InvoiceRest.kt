package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.ErrorResponse
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoiceRest(javalin: Javalin, private val invoiceService: InvoiceService) {
    private val log = KotlinLogging.logger {}
    init {
        javalin.routes {
            ApiBuilder.path("rest") {
                ApiBuilder.path("v1") {
                    ApiBuilder.path("invoices") {
                        ApiBuilder.get {
                            val status:String? = it.queryParam("status")
                            if (status == null) {
                                it.json(invoiceService.fetchAll())
                            } else {
                                runCatching { InvoiceStatus.valueOf(status) }
                                    .onSuccess { invoiceStatus: InvoiceStatus ->  it.json(invoiceService.fetchInvoicesByStatus(invoiceStatus)) }
                                    .onFailure { exception: Throwable ->
                                        log.error("$status status not found", exception)
                                        it.json(ErrorResponse("$status status not found")).status(404)}
                            }
                        }
                        ApiBuilder.get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}