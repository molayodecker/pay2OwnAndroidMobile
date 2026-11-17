package com.pay2own.restrictions

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import com.pay2own.domain.DeviceOwnerPolicies
import com.pay2own.domain.ReleaseResult
import com.pay2own.domain.ReleaseWorkScheduler
import com.pay2own.domain.RestrictionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class ReleaseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RestrictionRepository,
    private val policies: DeviceOwnerPolicies,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = releaseOnce(repository, policies)
}

internal suspend fun releaseOnce(
    repository: RestrictionRepository,
    policies: DeviceOwnerPolicies,
): Result {
    policies.clear()
    if (!policies.areClear()) return Result.retry()
    return when (repository.confirmRelease()) {
        ReleaseResult.Done, ReleaseResult.NotPending -> Result.success()
        ReleaseResult.Retry -> Result.retry()
    }
}

@Singleton
class WorkManagerReleaseScheduler @Inject constructor(
    private val workManager: WorkManager,
) : ReleaseWorkScheduler {
    override fun enqueue() {
        val request = OneTimeWorkRequestBuilder<ReleaseWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "release-confirmed"
    }
}
