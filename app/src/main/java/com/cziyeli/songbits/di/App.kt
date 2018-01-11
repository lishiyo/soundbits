package com.cziyeli.songbits.di

import android.app.Activity
import android.app.Application
import com.cziyeli.commons.di.UtilsModule
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.stetho.Stetho
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.reactivex.Observable
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

/**
 * Created by connieli on 12/31/17.
 */
class App : Application(), HasActivityInjector {
    companion object {
        lateinit var appComponent: ApplicationComponent

        fun nukeDatabase() {
            Observable.just(appComponent.tracksDatabase())
                    .subscribeOn(SchedulerProvider.io())
                    .subscribe { db -> db.tracksDao().nuke() }
        }

        fun getCurrentUserId() : String {
            if (appComponent.userManager().userId.isBlank()) {
                throw(Throwable("user id is blank! log in first?"))
            }

            return appComponent.userManager().userId
        }
    }

    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        initializeDagger()
        Fresco.initialize(this)
        Stetho.initializeWithDefaults(this);
    }

    fun initializeDagger() {
        appComponent = DaggerApplicationComponent.builder()
                .appModule(AppModule(this))
                .roomModule(RoomModule())
                .remoteModule(RemoteModule())
                .utilsModule(UtilsModule())
                .build()

        appComponent.inject(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityInjector
    }

}