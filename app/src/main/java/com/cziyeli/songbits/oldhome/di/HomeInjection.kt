package com.cziyeli.songbits.oldhome.di

import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import com.cziyeli.songbits.oldhome.HomeActivity
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector

/**
 * HomeActivity specific instances.
 *
 * Created by connieli on 12/31/17.
 */
@Module(includes = [ViewModelsModule::class])
class HomeModule

// Don't need this, just example of part 2: https://google.github.io/dagger/android.html
@Subcomponent(modules = [HomeModule::class])
interface HomeActivitySubComponent : AndroidInjector<HomeActivity> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<HomeActivity>()

}