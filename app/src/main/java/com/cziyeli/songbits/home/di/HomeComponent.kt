package com.cziyeli.songbits.home.di

import com.cziyeli.songbits.di.PerActivity
import com.cziyeli.songbits.home.HomeActivity
import com.cziyeli.songbits.home.HomeViewModel
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