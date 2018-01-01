package com.cziyeli.spotifydemo.di

import com.cziyeli.spotifydemo.home.HomeComponent
import com.cziyeli.spotifydemo.home.HomeModule
import dagger.Component
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Component(modules = [
    (AppModule::class),
    (UtilsModule::class),
    (RoomModule::class),
    (RemoteModule::class)
])
@Singleton
interface AppComponent {

    // injection targets
    fun inject(application: App)


    // subcomponents
    fun plus(homeModule: HomeModule): HomeComponent
}


