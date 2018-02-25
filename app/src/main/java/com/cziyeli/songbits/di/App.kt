package com.cziyeli.songbits.di

import android.app.Activity
import android.app.Application
import android.support.v4.content.res.ResourcesCompat
import com.cziyeli.commons.di.UtilsModule
import com.cziyeli.songbits.R
import com.facebook.stetho.Stetho
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import es.dmoral.toasty.Toasty
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
        Stetho.initializeWithDefaults(this)
        initializeToasty()
    }

    private fun initializeDagger() {
        appComponent = DaggerApplicationComponent.builder()
                .appModule(AppModule(this))
                .roomModule(RoomModule())
                .remoteModule(RemoteModule())
                .utilsModule(UtilsModule())
                .build()

        appComponent.inject(this)
    }

    private fun initializeToasty() {
        Toasty.Config.getInstance()
                .setSuccessColor(resources.getColor(R.color.venice_verde)) // optional
                .setTextColor(resources.getColor(R.color.colorWhite)) // optional
                .tintIcon(true) // optional (apply textColor also to the icon)
                .setTextSize(18) // optional
                .setToastTypeface(ResourcesCompat.getFont(this, R.font.indie_flower)!!)
                .apply() // required
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityInjector
    }

}