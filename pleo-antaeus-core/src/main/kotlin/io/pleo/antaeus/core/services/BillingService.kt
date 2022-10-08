package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    fun settleInvoice(){

    }
}
