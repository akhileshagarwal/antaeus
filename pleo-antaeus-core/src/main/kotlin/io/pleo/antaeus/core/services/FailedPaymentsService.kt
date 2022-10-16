package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.handlers.FailedPaymentHandler
import io.pleo.antaeus.core.lock.LocksHandler
import io.pleo.antaeus.models.InvoiceDLQ
import java.time.Duration
import java.util.*

class FailedPaymentsService(private val failedPaymentHandlers: List<FailedPaymentHandler>,
                            private val invoiceService: InvoiceService,
                            private val locksHandler: LocksHandler
) {
    private val lockKey = "failed-billing-lock"

    fun takeAction(){
        var unHandledInvoices = fetchPendingInvoicesAndUpdateStatusToBeingHandled()
        do{
            unHandledInvoices.forEach {
                failedPaymentHandlers
                    .first { failedPaymentHandler: FailedPaymentHandler -> failedPaymentHandler.isResponsibleFor() == it.failureReason }.handle(it)
            }
            unHandledInvoices = fetchPendingInvoicesAndUpdateStatusToBeingHandled()
        }while (unHandledInvoices.isNotEmpty())
    }

    private fun fetchPendingInvoicesAndUpdateStatusToBeingHandled(): List<InvoiceDLQ>{
        val lockIdentifier = UUID.randomUUID().toString()
        val lockDuration = Duration.ofMillis(2000)
        val acquireTimeout = Duration.ofMillis(500)
        var isSuccessful = false
        while(!isSuccessful){
            isSuccessful = locksHandler.tryLockWithTimeout(lockKey, lockDuration, acquireTimeout, lockIdentifier)
        }

        val unHandledInvoices = invoiceService.fetchLimitedInvoicesDLQByStatus()
        invoiceService.updateInvoiceDLQsToHandled(unHandledInvoices)

        locksHandler.releaseLock(lockKey, lockIdentifier)
        return unHandledInvoices
    }
}