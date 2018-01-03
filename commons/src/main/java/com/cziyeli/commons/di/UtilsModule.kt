package com.cziyeli.commons.di

import android.content.Context
import android.preference.PreferenceManager
import com.cziyeli.commons.AUTH_TOKEN
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
class UtilsModule {

    @Provides
    @Singleton
    fun provideSchedulerProvider(): BaseSchedulerProvider = SchedulerProvider

    @Provides
    @Named("accessToken")
    fun provideAccessToken(context: Context) : String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(AUTH_TOKEN, "")
    }
}