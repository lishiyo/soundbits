package com.cziyeli.songbits.cards.di

import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.Repository
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.CardsViewModelFactory
import com.cziyeli.songbits.di.AppModule
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named
import javax.inject.Singleton


/**
 * Created by connieli on 1/1/18.
 */
@Module
class CardsModule {

    @Provides
    @PerActivity
    fun provideTrackActionProcessor(repo: Repository,
                                    schedulerProvider: BaseSchedulerProvider): TrackActionProcessor {
        return TrackActionProcessor(repo, schedulerProvider)
    }


    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(activity: CardsActivity): Activity {
        return activity
    }

    @Provides
    @PerActivity
    fun providePlaylist(activity: CardsActivity): com.cziyeli.domain.playlists.Playlist {
        return activity.playlist
    }

    // See https://github.com/googlesamples/android-architecture-components/issues/207
    @Provides
    @PerActivity
    @Named("CardsViewModel")
    fun providesViewModelFactory(repository: RepositoryImpl,
                                 actionProcessor: TrackActionProcessor,
                                 schedulerProvider: BaseSchedulerProvider,
                                 playlist: Playlist): ViewModelProvider.Factory {
        return CardsViewModelFactory(
                repository,
                actionProcessor,
                schedulerProvider,
                playlist
        )
    }
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
