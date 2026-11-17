package com.pay2own.di

import com.pay2own.data.LocalRestrictionStore
import com.pay2own.data.Pay2OwnRepositoryImpl
import com.pay2own.data.RestrictionRepositoryImpl
import com.pay2own.data.RestrictionStore
import com.pay2own.domain.DeviceOwnerPolicies
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.domain.ReleaseWorkScheduler
import com.pay2own.domain.RestrictionRepository
import com.pay2own.domain.Sleeper
import com.pay2own.restrictions.AndroidDevicePolicyClient
import com.pay2own.restrictions.DevicePolicyClient
import com.pay2own.restrictions.FinancingDeviceOwnerPolicies
import com.pay2own.restrictions.WorkManagerReleaseScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(impl: Pay2OwnRepositoryImpl): Pay2OwnRepository

    @Binds
    abstract fun sleeper(impl: RealSleeper): Sleeper

    @Binds
    @Singleton
    abstract fun restrictionStore(impl: LocalRestrictionStore): RestrictionStore

    @Binds
    @Singleton
    abstract fun restrictions(impl: RestrictionRepositoryImpl): RestrictionRepository

    @Binds
    @Singleton
    abstract fun devicePolicyClient(impl: AndroidDevicePolicyClient): DevicePolicyClient

    @Binds
    @Singleton
    abstract fun devicePolicies(impl: FinancingDeviceOwnerPolicies): DeviceOwnerPolicies

    @Binds
    @Singleton
    abstract fun releaseWork(impl: WorkManagerReleaseScheduler): ReleaseWorkScheduler
}

class RealSleeper @Inject constructor() : Sleeper {
    override suspend fun sleep(millis: Long) = delay(millis)
}
