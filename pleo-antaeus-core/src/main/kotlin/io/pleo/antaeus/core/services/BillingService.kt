package io.pleo.antaeus.core.services

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.FailureReason.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import mu.KotlinLogging
import java.time.Duration

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val log = KotlinLogging.logger {}
    private val retryConfig = RetryConfig.custom<Any>()
        .maxAttempts(3)
        .waitDuration(Duration.ofSeconds(1))
        .retryExceptions(NetworkException::class.java)
        .failAfterMaxAttempts(true)
        .build()
    private val retry = RetryRegistry.of(retryConfig).retry("payment-retry")

    /**
     * Monthly Scheduler which fetches PENDING Invoices in a batch of 100 from Invoice Table
     * and mark them IN_PROGRESS. If the payment was successful then mark the invoice as PAID
     * otherwise create an entry in InvoiceDLQ with failureReason
     */
    fun settleInvoice() {
        var pendingInvoices = fetchPendingInvoicesAndUpdateStatusToInProgress()
        do {
            pendingInvoices.forEach {
                try {
                    val isPaymentSuccessful = Retry.decorateCheckedSupplier(retry) {
                        log.info { "Trying payment for invoice - ${it.id}" }
                        paymentProvider.charge(it)
                    }.apply()
                    if (isPaymentSuccessful) {
                        invoiceService.updateInvoiceStatus(it, PAID)
                    } else {
                        invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, INSUFFICIENT_FUNDS)
                    }
                } catch (e: NetworkException) {
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, NETWORK_FAILURE)
                } catch (e: CurrencyMismatchException) {
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, CURRENCY_MISMATCH)
                } catch (e: CustomerNotFoundException) {
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, CUSTOMER_NOT_FOUND)
                } catch (e: Exception) {
                    log.error { "Request Failed for ${it.id} due to $e" }
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, UNKNOWN)
                }
            }
            pendingInvoices = fetchPendingInvoicesAndUpdateStatusToInProgress()

        }while (pendingInvoices.isNotEmpty())
    }

    private fun fetchPendingInvoicesAndUpdateStatusToInProgress(): List<Invoice>{
        //Acquire Lock
        val pendingInvoices = invoiceService.fetchLimitedInvoicesByStatus(PENDING)
        invoiceService.updateInvoicesStatus(pendingInvoices, IN_PROGRESS)
        //Release Lock
        return pendingInvoices
    }

    /**
     * Fetches Records from DLQ and takes necessary actions based on the type of failure
     */
    fun retryFailedPayment() {

    }
}
