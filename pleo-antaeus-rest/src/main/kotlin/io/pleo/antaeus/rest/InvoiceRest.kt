package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.pleo.antaeus.core.services.InvoiceService

class InvoiceRest(javalin: Javalin, private val invoiceService: InvoiceService) {
    init {
        javalin.routes {
            ApiBuilder.path("rest") {
                ApiBuilder.path("v1") {
                    ApiBuilder.path("invoices") {
                        ApiBuilder.get {
                            it.json(invoiceService.fetchAll())
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