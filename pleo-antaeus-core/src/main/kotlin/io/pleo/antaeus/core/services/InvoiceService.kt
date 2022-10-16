/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.*

class InvoiceService(private val dal: AntaeusDal) {
    private val batchSize = 100
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchLimitedInvoicesByStatus(invoiceStatus: InvoiceStatus): List<Invoice> {
        return dal.fetchLimitedInvoicesWithStatus(batchSize, invoiceStatus)
    }

    fun fetchInvoicesByStatus(invoiceStatus: InvoiceStatus): List<Invoice> {
        return dal.fetchInvoicesWithStatus(invoiceStatus)
    }

    fun updateInvoicesStatus(invoices: List<Invoice>, invoiceStatus: InvoiceStatus){
        dal.updateInvoicesStatus(invoices, invoiceStatus.toString())
    }

    fun updateInvoiceStatus(invoice: Invoice, invoiceStatus: InvoiceStatus){
        dal.updateInvoiceStatus(invoice, invoiceStatus.toString())
    }

    fun updateInvoiceAndCreateInvoiceDLQ(invoice: Invoice, invoiceStatus: InvoiceStatus, failureReason: FailureReason){
        dal.updateInvoiceAndCreateInvoiceDLQ(invoice, invoiceStatus.toString(), failureReason.toString())
    }
}
