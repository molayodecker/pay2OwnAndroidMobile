package com.pay2own.restrictions

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pay2own.data.RestrictionStore
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
    override suspend fun doWork(): Result {
        val imei = inputData.getString(IMEI) ?: return Result.success()
        return releaseOnce(repository, policies, imei)
    }

    companion object {
        const val IMEI = "imei"
        const val TAG = "release-confirmed"
    }
}

/**
 * Removes financing restrictions only for the enrollment that queued the work,
 * and only after those restrictions are actually gone. A refusal from the server
 * puts the restrictions back.
 */
internal suspend fun releaseOnce(
    repository: RestrictionRepository,
    policies: DeviceOwnerPolicies,
    enrolledImei: String,
): Result {
    if (repository.enrolledImei() != enrolledImei) return Result.success()
    policies.clear()
    if (repository.enrolledImei() != enrolledImei) {
        policies.applyLock()
        return Result.success()
    }
    if (!policies.areClear()) {
        policies.applyLock()
        return Result.retry()
    }
    return when (repository.confirmRelease()) {
        ReleaseResult.Done -> Result.success()
        ReleaseResult.NotPending -> {
            policies.applyLock()
            Result.success()
        }
        ReleaseResult.Retry -> {
            policies.applyLock()
            Result.retry()
        }
    }
}

@Singleton
class WorkManagerReleaseScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val store: RestrictionStore,
) : ReleaseWorkScheduler {
    override suspend fun enqueue() {
        val imei = store.imei() ?: return
        val request = OneTimeWorkRequestBuilder<ReleaseWorker>()
            .setInputData(workDataOf(ReleaseWorker.IMEI to imei))
            .addTag(ReleaseWorker.TAG)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(workName(imei), ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        workManager.cancelAllWorkByTag(ReleaseWorker.TAG)
    }

    private fun workName(imei: String) = "release-confirmed-$imei"
}
