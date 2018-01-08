package com.cziyeli.songbits.di

import android.content.Context
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.data.Repository
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.data.local.RoomDataSource
import com.cziyeli.data.remote.RemoteDataSource
import com.cziyeli.domain.player.NativePlayerManager
import com.cziyeli.domain.player.PlayerInterface
import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Module
class AppModule(private val application: App) {

    @Provides
    @Singleton
    fun provideApp(): App = application

    @Provides
    @Singleton
    @ForApplication
    fun provideContext(): Context = application

    @Provides
    @Singleton
    fun provideRepository(local: RoomDataSource, remote: RemoteDataSource): Repository {
        return RepositoryImpl(local, remote)
    }

    @Provides
    @Singleton
    @Named("Native")
    fun provideNativePlayer(@ForApplication context: Context): PlayerInterface {
        return NativePlayerManager(context)
    }

    @Provides
    @Singleton
    fun provideSchedulerProvider(): BaseSchedulerProvider = SchedulerProvider

}

