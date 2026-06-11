package com.pricetag.scanner.di

import android.content.Context
import androidx.room.Room
import com.pricetag.scanner.data.db.AppDatabase
import com.pricetag.scanner.data.db.dao.JobDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "pricetag_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideJobDao(db: AppDatabase): JobDao = db.jobDao()
}
