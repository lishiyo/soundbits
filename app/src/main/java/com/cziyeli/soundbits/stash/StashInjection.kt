package com.cziyeli.soundbits.stash

import com.cziyeli.soundbits.di.viewModels.ViewModelsModule
import com.cziyeli.soundbits.root.RootModule
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
