package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService

class BillingRest(javalin: Javalin, private val billingService: BillingService) {
    init {
        javalin.routes {
            ApiBuilder.path("rest") {
                ApiBuilder.path("v1") {
                    ApiBuilder.path("billing") {
                        ApiBuilder.post {
                            it.json(billingService.settleInvoice())
                        }
                    }
                }
            }
        }
    }
}