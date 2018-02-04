package com.cziyeli.songbits.playlistcard.di

import android.app.Activity
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlistcard.PlaylistCardCreateActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateViewModel
import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named

/**
 * Created by connieli on 1/1/18.
 */
@Module
class PlaylistCardCreateModule {

    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(activity: PlaylistCardCreateActivity): Activity {
        return activity
    }

    @Provides
    @PerActivity
    fun providePendingTracks(activity: PlaylistCardCreateActivity): List<TrackModel> {
        return activity.pendingTracks // call inject after binding this in the activity!
    }

    @Provides
    @PerActivity
    fun provideInitialViewState(pendingTracks: List<TrackModel>): PlaylistCardCreateViewModel.ViewState {
        return PlaylistCardCreateViewModel.ViewState(pendingTracks = pendingTracks)
    }

    @Provides
    @PerActivity
    @Named("PlaylistCardCreateViewModel")
    fun providesViewModelFactory(repository: RepositoryImpl,
                                 actionProcessor: PlaylistCardCreateActionProcessor,
                                 schedulerProvider: BaseSchedulerProvider,
                                 initialViewState: PlaylistCardCreateViewModel.ViewState): ViewModelProvider.Factory {
        return PlaylistCardCreateViewModelFactory(
                repository,
                actionProcessor,
                schedulerProvider,
                initialViewState
        )
    }
}

// See https://github.com/googlesamples/android-architecture-components/issues/207
class PlaylistCardCreateViewModelFactory(val repository: RepositoryImpl,
                                         val actionProcessor: PlaylistCardCreateActionProcessor,
                                         val schedulerProvider: BaseSchedulerProvider,
                                         val initialViewState: PlaylistCardCreateViewModel.ViewState
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistCardCreateViewModel(repository, actionProcessor, schedulerProvider, initialViewState) as T
    }
}
