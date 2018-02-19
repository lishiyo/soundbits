package com.cziyeli.songbits.profile

import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import com.cziyeli.songbits.root.RootModule
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector

/**
 * Created by connieli on 2/18/18.
 */

@Module(includes = [ViewModelsModule::class, RootModule::class])
class ProfileModule


// Don't need this, just example of part 2: https://google.github.io/dagger/android.html
@Subcomponent(modules = [ProfileModule::class])
interface ProfileSubcomponent : AndroidInjector<ProfileFragment> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<ProfileFragment>()
}
