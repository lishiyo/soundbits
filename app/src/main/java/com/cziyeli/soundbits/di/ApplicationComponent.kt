package com.cziyeli.soundbits.di

import android.content.Context
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.commons.di.UtilsModule
import com.cziyeli.data.local.TracksDatabase
import com.cziyeli.soundbits.base.ChipsWidget
import com.cziyeli.soundbits.di.viewModels.ViewModelsModule
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
    ActivitiesModule::class, // binds all modules => activities
    FragmentsModule::class // binds all modules => fragments
])
@Singleton
interface ApplicationComponent {

    // ====== injection targets ========

    fun inject(application: App)

    fun inject(widget: ChipsWidget)

    // ====== GLOBAL ========

    // downstream components need these exposed
    // the method name does not matter, only the return type
    fun tracksDatabase() : TracksDatabase

    fun userManager() : com.cziyeli.domain.user.UserManager

    @ForApplication
    fun appContext() : Context
}


