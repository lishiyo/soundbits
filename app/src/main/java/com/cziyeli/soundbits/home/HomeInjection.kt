package com.cziyeli.soundbits.home

import com.cziyeli.soundbits.di.viewModels.ViewModelsModule
import com.cziyeli.soundbits.root.RootModule
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector

@Module(includes = [ViewModelsModule::class, RootModule::class])
class HomeModule

// Don't need this, just example of part 2: https://google.github.io/dagger/android.html
@Subcomponent(modules = [HomeModule::class])
interface HomeSubcomponent : AndroidInjector<HomeFragment> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<HomeFragment>()
}
