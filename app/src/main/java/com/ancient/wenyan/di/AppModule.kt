package com.ancient.wenyan.di

import android.content.Context
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.data.db.AppDatabase
import com.ancient.wenyan.domain.fsrs.FSRSEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFSRSEngine(): FSRSEngine {
        return FSRSEngine()
    }

    @Provides
    @Singleton
    fun provideWenYanRepository(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        fsrsEngine: FSRSEngine,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): WenYanRepository {
        return WenYanRepository(
            context = context,
            fsrsEngine = fsrsEngine,
            database = appDatabase,
            defaultDispatcher = defaultDispatcher,
            ioDispatcher = ioDispatcher
        )
    }
}
