package com.cziyeli.commons.di

import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Module
class UtilsModule {

    @Provides
    @Singleton
    fun provideSchedulerProvider(): BaseSchedulerProvider = SchedulerProvider

}