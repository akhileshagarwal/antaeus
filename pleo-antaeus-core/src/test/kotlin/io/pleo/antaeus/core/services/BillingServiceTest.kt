package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.lock.RedisLock
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.FailureReason.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.*
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private var invoice1: Invoice = getTestInvoice(1, PENDING)
    private var invoice2: Invoice = getTestInvoice(2, PENDING)

    private val invoiceService = mockk<InvoiceService>()
    private val paymentProvider = mockk<PaymentProvider>()
    private val redisLock = mockk<RedisLock>()

    private val billingService = BillingService(paymentProvider, invoiceService, redisLock)

    @BeforeEach
    fun setup(){
        every { invoiceService.updateInvoicesStatus(listOf(), IN_PROGRESS) } returns Unit
        every { redisLock.tryLockWithTimeout(any(), any(), any(), any()) } returns true
        every { redisLock.releaseLock(any(), any()) } returns true
    }

    @Test
    fun `will mark invoice status to PAID if payment is successful`() {
        val invoices = listOf(invoice1, invoice2)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { invoiceService.updateInvoiceStatus(invoice1, PAID) } returns Unit
        every { invoiceService.updateInvoiceStatus(invoice2, PAID) } returns Unit
        every { paymentProvider.charge(any()) } returns true

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceStatus(invoice1, PAID)
            invoiceService.updateInvoiceStatus(invoice2, PAID)
        }
    }

    @Test
    fun `will mark invoice status to FAILED if payment is unsuccessful due to insufficient funds`() {
        val invoices = listOf(invoice1)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { paymentProvider.charge(any()) } returns false
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, INSUFFICIENT_FUNDS) } returns Unit

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, INSUFFICIENT_FUNDS)
        }
    }

    @Test
    fun `will retry payment 3 times if external provider throws Network Exception`() {
        val invoices = listOf(invoice1)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { paymentProvider.charge(any()) } throws NetworkException()
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, NETWORK_FAILURE) } returns Unit

        billingService.settleInvoice()

        verify(exactly = 3, timeout = 1000) {
            paymentProvider.charge(invoice1)
        }
        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, NETWORK_FAILURE)
        }
    }

    @Test
    fun `will create an entry in InvoiceDLQ and fail the payment if payment provider throws CurrencyMismatchException`() {
        val invoices = listOf(invoice1)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(invoice1.id, invoice1.customerId)
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, CURRENCY_MISMATCH) } returns Unit

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            paymentProvider.charge(invoice1)
        }
        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, CURRENCY_MISMATCH)
        }
    }

    @Test
    fun `will create an entry in InvoiceDLQ and fail the payment if payment provider does not find customer`() {
        val invoices = listOf(invoice1)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { paymentProvider.charge(any()) } throws CustomerNotFoundException(invoice1.customerId)
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, CUSTOMER_NOT_FOUND) } returns Unit

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            paymentProvider.charge(invoice1)
        }
        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, CUSTOMER_NOT_FOUND)
        }
    }

    @Test
    fun `will create an entry in InvoiceDLQ and fail the payment if payment provider throws any unknown exception`() {
        val invoices = listOf(invoice1)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { paymentProvider.charge(any()) } throws IllegalArgumentException(invoice1.toString())
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, UNKNOWN) } returns Unit

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            paymentProvider.charge(invoice1)
        }
        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice1, FAILED, UNKNOWN)
        }
    }

    @Test
    fun `will mark invoice status to PAID if payment is successful and failed if unsuccessful`() {
        val invoices = listOf(invoice1, invoice2)
        every { invoiceService.fetchLimitedInvoicesByStatus(PENDING) } returns invoices andThen listOf()
        every { invoiceService.updateInvoicesStatus(invoices, IN_PROGRESS) } returns Unit
        every { invoiceService.updateInvoiceStatus(invoice1, PAID) } returns Unit
        every { invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice2, FAILED, INSUFFICIENT_FUNDS) } returns Unit
        every { paymentProvider.charge(invoice1) } returns true
        every { paymentProvider.charge(invoice2) } returns false

        billingService.settleInvoice()

        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceStatus(invoice1, PAID)
        }
        verify(exactly = 1, timeout = 1000) {
            invoiceService.updateInvoiceAndCreateInvoiceDLQ(invoice2, FAILED, INSUFFICIENT_FUNDS)
        }
    }

    private fun getTestInvoice(id:Int, invoiceStatus: InvoiceStatus): Invoice{
        return Invoice(id, 1, Money(BigDecimal.TEN, Currency.DKK), invoiceStatus)
    }
}