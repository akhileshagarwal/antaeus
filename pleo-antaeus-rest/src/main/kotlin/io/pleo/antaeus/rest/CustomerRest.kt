package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.pleo.antaeus.core.services.CustomerService

class CustomerRest(javalin: Javalin, private val customerService: CustomerService) {
    init {
        javalin.routes {
            ApiBuilder.path("rest") {
                ApiBuilder.path("v1") {
                    ApiBuilder.path("customers") {
                        ApiBuilder.get {
                            it.json(customerService.fetchAll())
                        }
                        ApiBuilder.get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}