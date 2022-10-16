package io.pleo.antaeus.core.job

import io.pleo.antaeus.core.services.BillingService
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

const val JOB_GROUP = "pleo-jobs"
const val JOB_NAME = "antaeus-billing"
private const val JOB_TRIGGER_NAME = "antaeus-billing-trigger"
private const val JOB_SCHEDULE = "0 0 0 1 * ?"

class BillingJobScheduler(
    private val billingService: BillingService
) {
    fun schedule() {
        val billingJob = JobBuilder
            .newJob()
            .withIdentity(JOB_NAME, JOB_GROUP)
            .ofType(BillingJob::class.java)
            .usingJobData(
                JobDataMap(
                    mapOf("billingService" to billingService)
                )
            ).build()

        val billingJobTrigger = TriggerBuilder
            .newTrigger()
            .withIdentity(JOB_TRIGGER_NAME, JOB_GROUP)
            .withSchedule(cronSchedule(JOB_SCHEDULE))
            .forJob(JOB_NAME, JOB_GROUP)
            .build()

        val jobScheduler = StdSchedulerFactory().scheduler
        jobScheduler.start()
        jobScheduler.scheduleJob(billingJob, billingJobTrigger)
    }
}