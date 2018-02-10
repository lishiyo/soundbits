package com.cziyeli.songbits.stash

import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import com.cziyeli.songbits.root.RootModule
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector

@Module(includes = [ViewModelsModule::class, RootModule::class])
class StashModule


// Don't need this, just example of part 2: https://google.github.io/dagger/android.html
@Subcomponent(modules = [StashModule::class])
interface StashSubcomponent : AndroidInjector<StashFragment> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<StashFragment>()
}
