package com.pricetag.scanner.di

import com.pricetag.scanner.data.repository.JobRepositoryImpl
import com.pricetag.scanner.data.repository.SettingsRepositoryImpl
import com.pricetag.scanner.domain.repository.JobRepository
import com.pricetag.scanner.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
