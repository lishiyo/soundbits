package com.cziyeli.songbits.di.viewModels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.songbits.home.HomeViewModel
import com.cziyeli.songbits.root.RootViewModel
import com.cziyeli.songbits.stash.StashViewModel
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
    abstract fun bindViewModelFactory(factory: ViewModelFactory) : ViewModelProvider.Factory
}