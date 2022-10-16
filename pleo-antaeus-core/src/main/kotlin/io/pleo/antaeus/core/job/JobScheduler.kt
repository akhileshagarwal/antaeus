package io.pleo.antaeus.core.job

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.FailedPaymentsService
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

const val JOB_GROUP = "pleo-jobs"
const val BILLING_JOB_NAME = "antaeus-billing"
const val FAILED_INVOICES_JOB_NAME = "antaeus-failed-invoices"
private const val BILLING_JOB_TRIGGER_NAME = "antaeus-billing-trigger"
private const val FAILED_INVOICES_JOB_TRIGGER_NAME = "antaeus-failed-invoices-trigger"
private const val BILLING_JOB_SCHEDULE = "0 0 0 1 * ?" // This should come from a config file
private const val FAILED_INVOICES_JOB_SCHEDULE = "0 0 0 1 * ?" // This should come from a config file

class JobScheduler(
    private val billingService: BillingService,
    private val failedPaymentsService: FailedPaymentsService
) {
    fun schedule() {
        val jobScheduler = StdSchedulerFactory().scheduler
        jobScheduler.start()

        scheduleBillingJob(jobScheduler)
        scheduleHandleFailedInvoicesJob(jobScheduler)
    }

    private fun scheduleHandleFailedInvoicesJob(scheduler: Scheduler){
        val failedInvoicesJob = JobBuilder
            .newJob()
            .withIdentity(FAILED_INVOICES_JOB_NAME, JOB_GROUP)
            .ofType(FailedInvoicesJob::class.java)
            .usingJobData(
                JobDataMap(
                    mapOf("failedPaymentsService" to failedPaymentsService)
                )
            ).build()

        val failedInvoicesJobTrigger = TriggerBuilder
            .newTrigger()
            .withIdentity(FAILED_INVOICES_JOB_TRIGGER_NAME, JOB_GROUP)
            .withSchedule(cronSchedule(FAILED_INVOICES_JOB_SCHEDULE))
            .forJob(FAILED_INVOICES_JOB_NAME, JOB_GROUP)
            .build()

        scheduler.scheduleJob(failedInvoicesJob, failedInvoicesJobTrigger)
    }

    private fun scheduleBillingJob(scheduler: Scheduler){
        val billingJob = JobBuilder
            .newJob()
            .withIdentity(BILLING_JOB_NAME, JOB_GROUP)
            .ofType(BillingJob::class.java)
            .usingJobData(
                JobDataMap(
                    mapOf("billingService" to billingService)
                )
            ).build()

        val billingJobTrigger = TriggerBuilder
            .newTrigger()
            .withIdentity(BILLING_JOB_TRIGGER_NAME, JOB_GROUP)
            .withSchedule(cronSchedule(BILLING_JOB_SCHEDULE))
            .forJob(BILLING_JOB_NAME, JOB_GROUP)
            .build()

        scheduler.scheduleJob(billingJob, billingJobTrigger)
    }
}