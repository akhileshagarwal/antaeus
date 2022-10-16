package io.pleo.antaeus.core.services

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.lock.LocksHandler
import io.pleo.antaeus.models.FailureReason.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import mu.KotlinLogging
import java.time.Duration
import java.util.*

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val locksHandler: LocksHandler
) {
    private val log = KotlinLogging.logger {}
    private val lockKey = "billing-lock"
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
                        paymentProvider.charge(it)
                    }.apply()
                    if (isPaymentSuccessful) {
                        log.info { "Payment successful for invoice - ${it.id}" }
                        invoiceService.updateInvoiceStatus(it, PAID)
                    } else {
                        log.info { "Insufficient Funds for invoice - ${it.id}. Creating Entry for DLQ" }
                        invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, INSUFFICIENT_FUNDS)
                    }
                } catch (e: NetworkException) {
                    // There can be a circuit breaker implementation as well which stops the job if this is happening a lot
                    log.error("Exception Occurred while contacting the PSP network", e)
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, NETWORK_FAILURE)
                } catch (e: CurrencyMismatchException) {
                    log.error("Currency Mismatch for invoice ${it.id} with customer ${it.customerId}", e)
                    invoiceService.updateInvoiceAndCreateInvoiceDLQ(it, FAILED, CURRENCY_MISMATCH)
                } catch (e: CustomerNotFoundException) {
                    //Maybe all the future invoices for this customer will fail as it is not Found. So, we should exclude charging invoices for this customer
                    log.error("Customer not found for invoice ${it.id} with customer ${it.customerId}", e)
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
        val lockIdentifier = UUID.randomUUID().toString()
        val lockDuration = Duration.ofMillis(2000)
        val acquireTimeout = Duration.ofMillis(500)
        var isSuccessful = false
        while(!isSuccessful){
            isSuccessful = locksHandler.tryLockWithTimeout(lockKey, lockDuration, acquireTimeout, lockIdentifier)
        }

        val pendingInvoices = invoiceService.fetchLimitedInvoicesByStatus(PENDING)
        invoiceService.updateInvoicesStatus(pendingInvoices, IN_PROGRESS)

        locksHandler.releaseLock(lockKey, lockIdentifier)
        return pendingInvoices
    }
}
