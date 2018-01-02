package com.cziyeli.songbits.di

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco

/**
 * Created by connieli on 12/31/17.
 */
class App : Application() {
    companion object {
        lateinit var appComponent: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        initializeDagger()
        Fresco.initialize(this)
    }

    fun initializeDagger() {
        appComponent = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .roomModule(RoomModule())
                .remoteModule(RemoteModule())
                .utilsModule(UtilsModule())
                .build()
    }
}