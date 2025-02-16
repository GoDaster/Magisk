package com.topjohnwu.magisk.core

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.Context
import androidx.core.content.getSystemService
import com.topjohnwu.magisk.BuildConfig
import com.topjohnwu.magisk.core.base.BaseJobService
import com.topjohnwu.magisk.di.ServiceLocator
import com.topjohnwu.magisk.view.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class JobService : BaseJobService() {

    private val job = Job()
    private val svc get() = ServiceLocator.networkService

    override fun onStartJob(params: JobParameters): Boolean {
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            svc.fetchUpdate()?.run {
                if (Info.env.isActive && BuildConfig.VERSION_CODE < magisk.versionCode)
                    Notifications.managerUpdate(this@JobService)
            }
            jobFinished(params, false)
        }
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job.cancel()
        return false
    }

    companion object {
        fun schedule(context: Context) {
            val svc = context.getSystemService<JobScheduler>() ?: return
            if (Config.checkUpdate) {
                val cmp = JobService::class.java.cmp(context.packageName)
                val info = JobInfo.Builder(Const.ID.JOB_SERVICE_ID, cmp)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(true)
                    .build()
                svc.schedule(info)
            } else {
                svc.cancel(Const.ID.JOB_SERVICE_ID)
            }
        }
    }
}
