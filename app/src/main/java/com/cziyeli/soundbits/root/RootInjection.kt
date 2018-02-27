package com.cziyeli.soundbits.root

import com.cziyeli.commons.di.PerActivity
import com.cziyeli.soundbits.di.viewModels.ViewModelsModule
import com.jakewharton.rxrelay2.PublishRelay
import dagger.Module
import dagger.Provides
import io.reactivex.Observable


@Module(includes = [ViewModelsModule::class])
class RootModule {

    @PerActivity
    @Provides
    fun provideRootViewStatesStream() : Observable<RootViewState> {
        return PublishRelay.create<RootViewState>()
    }

    @PerActivity
    @Provides
    fun provideRootEventsPublisher() : PublishRelay<RootIntent> {
        return PublishRelay.create()
    }
}