package com.cziyeli.soundbits.di.viewModels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.soundbits.home.HomeViewModel
import com.cziyeli.soundbits.profile.ProfileViewModel
import com.cziyeli.soundbits.root.RootViewModel
import com.cziyeli.soundbits.stash.StashViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Bind all non-custom view models here.
 * If viewmodel needs to be created custom parameters, see example like [CardsModule]
 */
@Module
abstract class ViewModelsModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(viewModel: HomeViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RootViewModel::class)
    abstract fun bindRootViewModel(viewModel: RootViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StashViewModel::class)
    abstract fun bindStashViewModel(viewModel: StashViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel) : ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory) : ViewModelProvider.Factory
}