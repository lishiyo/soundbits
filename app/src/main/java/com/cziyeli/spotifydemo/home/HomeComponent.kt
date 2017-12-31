package com.cziyeli.spotifydemo.home

import dagger.Subcomponent

/**
 * Created by connieli on 12/31/17.
 */
@Subcomponent(modules = [(HomeModule::class)])
interface HomeComponent {
    fun inject(activity: HomeActivity)
}