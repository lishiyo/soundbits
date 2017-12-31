package com.cziyeli.spotifydemo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
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
    fun provideContext(): Context = application

    @Provides
    @Named("io")
    @Singleton
    fun ioScheduler(): Scheduler = Schedulers.io()

    @Provides
    @Named("main")
    @Singleton
    fun mainThreadScheduler(): Scheduler = AndroidSchedulers.mainThread()
}

