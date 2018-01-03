package com.cziyeli.songbits.cards.di

import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.CardsViewModel
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * Created by connieli on 1/1/18.
 */
@Module
class CardsModule {

    @Provides
    @PerActivity
    fun provideActionProcessor(repo: Repository, schedulerProvider: BaseSchedulerProvider)
            : TrackActionProcessor {
        return TrackActionProcessor(repo, schedulerProvider)
    }

}

@Subcomponent(modules = [(CardsModule::class)])
@PerActivity
interface CardsComponent {
    fun inject(activity: CardsActivity)

    fun inject(viewModel: CardsViewModel)
}

