package com.cziyeli.soundbits.di

import com.cziyeli.commons.di.PerActivity
import com.cziyeli.commons.di.PerFragment
import com.cziyeli.soundbits.MainActivity
import com.cziyeli.soundbits.cards.CardsActivity
import com.cziyeli.soundbits.cards.di.CardsModule
import com.cziyeli.soundbits.di.viewModels.ViewModelsModule
import com.cziyeli.soundbits.home.HomeFragment
import com.cziyeli.soundbits.home.HomeModule
import com.cziyeli.soundbits.home.HomeSubcomponent
import com.cziyeli.soundbits.playlistcard.PlaylistCardActivity
import com.cziyeli.soundbits.playlistcard.create.PlaylistCardCreateActivity
import com.cziyeli.soundbits.playlistcard.di.PlaylistCardCreateModule
import com.cziyeli.soundbits.playlistcard.di.PlaylistCardModule
import com.cziyeli.soundbits.profile.ProfileFragment
import com.cziyeli.soundbits.profile.ProfileModule
import com.cziyeli.soundbits.root.RootActivity
import com.cziyeli.soundbits.root.RootModule
import com.cziyeli.soundbits.stash.StashFragment
import com.cziyeli.soundbits.stash.StashModule
import com.cziyeli.soundbits.stash.StashSubcomponent
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Binds all activities and fragments to their corresponding modules.
 *
 * Created by connieli on 1/2/18.
 */
@Module(subcomponents = [
//        OldHomeActivitySubComponent::class
])
abstract class ActivitiesModule {

    @PerActivity
    @ContributesAndroidInjector(
            modules = [ViewModelsModule::class]
    )
    abstract fun provideMainActivity(): MainActivity

    // Approach #1
//    @Binds
//    @IntoMap
//    @ActivityKey(OldHomeActivity::class)
//    internal abstract fun provideHomeActivity(builder: OldHomeActivitySubComponent.Builder): AndroidInjector.Factory<out Activity>

    // Approach #2
    @PerActivity
    @ContributesAndroidInjector(
            modules = [CardsModule::class]
    )
    abstract fun provideCardsActivity(): CardsActivity

    @PerActivity
    @ContributesAndroidInjector(
            modules = [RootModule::class]
    )
    abstract fun provideRootActivity(): RootActivity

    @PerActivity
    @ContributesAndroidInjector(
            modules = [PlaylistCardModule::class]
    )
    abstract fun providePlaylistCardActivity(): PlaylistCardActivity

    @PerActivity
    @ContributesAndroidInjector(
            modules = [PlaylistCardCreateModule::class]
    )
    abstract fun providePlaylistCardCreateActivity(): PlaylistCardCreateActivity
}

@Module(subcomponents = [
    HomeSubcomponent::class,
    StashSubcomponent::class
])
abstract class FragmentsModule {

    @PerFragment
    @ContributesAndroidInjector(
            modules = [(HomeModule::class)]
    )
    internal abstract fun homeFragmentInjector(): HomeFragment

    @PerFragment
    @ContributesAndroidInjector(
            modules = [(StashModule::class)]
    )
    internal abstract fun stashFragmentInjector(): StashFragment

    @PerFragment
    @ContributesAndroidInjector(
            modules = [(ProfileModule::class)]
    )
    internal abstract fun profileFragmentInjector(): ProfileFragment
}