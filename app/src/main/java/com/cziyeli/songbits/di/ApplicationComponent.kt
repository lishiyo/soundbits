package com.cziyeli.songbits.di

import com.cziyeli.commons.di.UtilsModule
import com.cziyeli.data.local.TracksDatabase
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Component(modules = [
    AndroidInjectionModule::class,
    ViewModelsModule::class, // binds all view models
    AppModule::class,
    UtilsModule::class,
    RoomModule::class,
    RemoteModule::class,
    ActivitiesModule::class // binds all subcomponents => activities
])
@Singleton
interface ApplicationComponent {

    // injection targets
    fun inject(application: App)

    // downstream components need these exposed
    // the method name does not matter, only the return type
    fun tracksDatabase() : TracksDatabase
}


