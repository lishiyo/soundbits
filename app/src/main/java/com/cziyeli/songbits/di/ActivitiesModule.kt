package com.cziyeli.songbits.di

import android.app.Activity
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.songbits.MainActivity
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.di.CardsModule
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import com.cziyeli.songbits.oldhome.HomeActivity
import com.cziyeli.songbits.oldhome.di.HomeActivitySubComponent
import dagger.Binds
import dagger.Module
import dagger.android.ActivityKey
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

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
    @ContributesAndroidInjector(
            modules = [ViewModelsModule::class]
    )
    abstract fun provideMainActivity(): MainActivity

    // Approach #1
    @Binds
    @IntoMap
    @ActivityKey(HomeActivity::class)
    internal abstract fun provideHomeActivity(builder: HomeActivitySubComponent.Builder): AndroidInjector.Factory<out Activity>

    // Approach #2
    @PerActivity
    @ContributesAndroidInjector(
            modules = [CardsModule::class]
    )
    abstract fun provideCardsActivity(): CardsActivity
}