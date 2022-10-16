package io.pleo.antaeus.core.job

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

private val log = KotlinLogging.logger {}

class BillingJob(var billingService: BillingService) : Job {

    override fun execute(context: JobExecutionContext?) {
        log.info { "Starting $BILLING_JOB_NAME in $JOB_GROUP group" }
        billingService.settleInvoice()
    }
}