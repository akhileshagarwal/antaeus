package io.pleo.antaeus.core.job

import io.pleo.antaeus.core.services.FailedPaymentsService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext

private val log = KotlinLogging.logger {}

class FailedInvoicesJob(private val failedPaymentsService: FailedPaymentsService) : Job {

    override fun execute(context: JobExecutionContext?) {
        log.info { "Starting $FAILED_INVOICES_JOB_NAME in $JOB_GROUP group" }
        failedPaymentsService.takeAction()
    }
}