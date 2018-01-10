package com.cziyeli.songbits.di

import com.cziyeli.commons.di.PerActivity
import com.cziyeli.songbits.MainActivity
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.di.CardsModule
import com.cziyeli.songbits.home.HomeActivity
import com.cziyeli.songbits.home.di.HomeActivitySubComponent
import dagger.Module
import dagger.android.ContributesAndroidInjector



/**
 * Binds all activity subcomponents.
 *
 * Created by connieli on 1/2/18.
 */
@Module(subcomponents = [
        HomeActivitySubComponent::class
])
abstract class ActivitiesModule {

    @PerActivity
    @ContributesAndroidInjector
    abstract fun provideMainActivity(): MainActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun provideHomeActivity(): HomeActivity

    @PerActivity
    @ContributesAndroidInjector(
            modules = [CardsModule::class]
    )
    abstract fun provideCardsActivity(): CardsActivity
}