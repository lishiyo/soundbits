package com.cziyeli.songbits.di.viewModels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.songbits.cards.CardsViewModel
import com.cziyeli.songbits.cards.summary.SummaryViewModel
import com.cziyeli.songbits.home.HomeViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Bind all view models here.
 */
@Module
abstract class ViewModelsModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(viewModel: HomeViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CardsViewModel::class)
    abstract fun bindCardsViewModel(viewModel: CardsViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SummaryViewModel::class)
    abstract fun bindSummaryViewModel(viewModel: SummaryViewModel) : ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory) : ViewModelProvider.Factory
}