package com.cziyeli.songbits.home

import com.cziyeli.songbits.di.PerActivity
import dagger.Subcomponent

/**
 * Created by connieli on 12/31/17.
 */
@Subcomponent(modules = [(HomeModule::class)])
@PerActivity
interface HomeComponent {
    fun inject(activity: HomeActivity)

    fun inject(viewModel: HomeViewModel)
}