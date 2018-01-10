package com.cziyeli.songbits.cards.di

import android.app.Activity
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.commons.di.UtilsModule
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.di.AppModule
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by connieli on 1/1/18.
 */
@Module(includes = [ViewModelsModule::class, AppModule::class])
class CardsModule {

    @Provides
    @Singleton
    fun provideTrackActionProcessor(repo: Repository,
                                    schedulerProvider: BaseSchedulerProvider): TrackActionProcessor {
        return TrackActionProcessor(repo, schedulerProvider)
    }


    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(cardsActivity: CardsActivity): Activity {
        return cardsActivity
    }

    @Provides
    @PerActivity
    fun providePlaylistId(@Named("ActivityContext") cardsActivity: CardsActivity) : com.cziyeli.domain.playlists.Playlist {
        return cardsActivity.playlist
    }

//    @Provides
//    @PerActivity
//    fun provideSpotifyPlayerManager(@Named("ActivityContext") activity: Activity, accessToken: String) : SpotifyPlayerManager {
//        return SpotifyPlayerManager(activity, accessToken)
//    }
}

@Module(includes = [ViewModelsModule::class, AppModule::class])
class SummaryModule {

    @Provides
    @Singleton
    fun provideActionProcessor(repo: Repository,
                               schedulerProvider: BaseSchedulerProvider): SummaryActionProcessor {
        return SummaryActionProcessor(repo, schedulerProvider)
    }
}

@Subcomponent(modules = [CardsModule::class, SummaryModule::class, UtilsModule::class])
interface CardsActivitySubComponent : AndroidInjector<CardsActivity> {

    @Subcomponent.Builder
    abstract class builder : AndroidInjector.Builder<CardsActivity>()

}
