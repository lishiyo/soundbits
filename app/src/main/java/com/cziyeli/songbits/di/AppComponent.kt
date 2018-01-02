package com.cziyeli.songbits.di

import com.cziyeli.songbits.home.di.HomeComponent
import com.cziyeli.songbits.home.di.HomeModule
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


