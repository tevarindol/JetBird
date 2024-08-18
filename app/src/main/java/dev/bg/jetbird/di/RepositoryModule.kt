package dev.bg.jetbird.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.LogRepositoryImpl
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.PreferencesRepositoryImpl
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.repository.VpnRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideLogRepository(
        logRepository: LogRepositoryImpl
    ): LogRepository

    @Binds
    @Singleton
    abstract fun providePreferencesRepository(
        preferencesRepository: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun provideVpnRepository(
        vpnRepositoryImpl: VpnRepositoryImpl
    ): VpnRepository

}
