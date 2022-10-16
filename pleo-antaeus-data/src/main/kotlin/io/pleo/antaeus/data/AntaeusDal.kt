/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.InvoiceStatus.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchLimitedInvoicesWithStatus(count: Int, invoiceStatus: InvoiceStatus): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .select { InvoiceTable.status.eq(invoiceStatus.toString()) }
                .limit(count, offset = 0)
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesWithStatus(invoiceStatus: InvoiceStatus): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .select { InvoiceTable.status.eq(invoiceStatus.toString()) }
                .map { it.toInvoice() }
        }
    }

    fun updateInvoicesStatus(invoices: List<Invoice>, status: String) {
        transaction(db) {
            // Insert the invoice and returns its new id.
            invoices.forEach {
                InvoiceTable
                    .update({ InvoiceTable.id eq it.id }) {
                        it[this.status] = status
                    }
            }
        }
    }

    fun updateInvoiceStatus(invoice: Invoice, status: String) {
        transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id eq invoice.id }) {
                    it[this.status] = status
                }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoiceAndCreateInvoiceDLQ(invoice: Invoice, invoiceStatus: String, failureReason: String) {
        transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id eq invoice.id }) {
                    it[this.status] = invoiceStatus
                }
            InvoiceDLQTable
                .insert {
                    it[this.invoiceId] = invoice.id
                    it[this.failureReason] = failureReason
                }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return CustomerTable
            .selectAll()
            .map { it.toCustomer() }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    /**
     * This should have been created in a separate Dal file so that
     * we don't end up having all the database logic in one file as
     * it becomes really hard to maintain and test the class moving forward.
     *
     * The same goes for existing logic in here.
     */
    fun fetchInvoicesDLQ(failureReason: FailureReason): List<InvoiceDLQ> {
        return InvoiceDLQTable
            .select { InvoiceDLQTable.failureReason.eq(failureReason.toString()) }
            .map { it.toInvoiceDLQ() }
    }

    fun updateInvoiceDLQIsHandled(invoiceDLQ: InvoiceDLQ, isHandled: Boolean) {
        transaction(db) {
            InvoiceDLQTable
                .update({ InvoiceDLQTable.id eq invoiceDLQ.id }) {
                    it[this.isHandled] = isHandled
                }
        }
    }

    fun fetchAllInvoicesDLQ(): List<InvoiceDLQ> {
        return transaction(db) {
            InvoiceDLQTable
                .selectAll()
                .map { it.toInvoiceDLQ() }
        }
    }

    fun fetchLimitedInvoicesDLQ(count: Int): List<InvoiceDLQ> {
        return transaction(db) {
            InvoiceDLQTable
                .select { InvoiceDLQTable.isHandled.eq(false) }
                .limit(count, offset = 0)
                .map { it.toInvoiceDLQ() }
        }
    }
}
