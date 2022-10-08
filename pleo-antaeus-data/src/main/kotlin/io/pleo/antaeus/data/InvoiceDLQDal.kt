package io.pleo.antaeus.data

import io.pleo.antaeus.models.FailureReason
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceDLQ
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class InvoiceDLQDal(private val db: Database) {

    fun fetchInvoicesDLQ(failureReason: FailureReason): List<InvoiceDLQ>? {
        return InvoiceDLQTable
            .select { InvoiceDLQTable.failureReason.eq(failureReason.toString()) }
            .map { it.toInvoiceDLQ() }
    }

    fun createInvoiceDLQ(invoice: Invoice, failureReason: FailureReason) {
        InvoiceDLQTable
            .insert {
                it[this.invoiceId] = invoice.id
                it[this.failureReason] = failureReason.toString()
            }
    }
}