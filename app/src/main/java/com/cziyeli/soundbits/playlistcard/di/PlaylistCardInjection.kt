package com.cziyeli.soundbits.playlistcard.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.domain.playlistcard.PlaylistCardActionProcessor
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.soundbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.soundbits.playlistcard.PlaylistCardActivity
import com.cziyeli.soundbits.playlistcard.PlaylistCardViewModel
import dagger.Module
import dagger.Provides
import kotlinx.android.synthetic.main.widget_expandable_tracks.*
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named



/**
 * Created by connieli on 1/1/18.
 */
@Module
class PlaylistCardModule {

    @Provides
    @PerActivity
    fun provideTracksRecyclerViewDelegate(@Named("ActivityContext") activity: PlaylistCardActivity): TracksRecyclerViewDelegate {
        return TracksRecyclerViewDelegate(activity, activity.tracks_recycler_view, activity)
    }

    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(activity: PlaylistCardActivity): PlaylistCardActivity {
        return activity
    }

    @Provides
    @PerActivity
    fun providePlaylist(activity: PlaylistCardActivity): Playlist {
        return activity.playlist // call inject after binding this in the activity!
    }

    @Provides
    @PerActivity
    fun provideInitialViewState(playlist: Playlist): PlaylistCardViewModel.PlaylistCardViewState {
        return PlaylistCardViewModel.PlaylistCardViewState(playlist = playlist)
    }

    @Provides
    @PerActivity
    @Named("PlaylistCardViewModel")
    fun providesViewModelFactory(actionProcessor: PlaylistCardActionProcessor,
                                 schedulerProvider: BaseSchedulerProvider,
                                 initialViewState: PlaylistCardViewModel.PlaylistCardViewState): ViewModelProvider.Factory {
        return PlaylistCardViewModelFactory(
                actionProcessor,
                schedulerProvider,
                initialViewState
        )
    }
}

// See https://github.com/googlesamples/android-architecture-components/issues/207
class PlaylistCardViewModelFactory(val actionProcessor: PlaylistCardActionProcessor,
                                   val schedulerProvider: BaseSchedulerProvider,
                                   val initialViewState: PlaylistCardViewModel.PlaylistCardViewState
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlaylistCardViewModel(actionProcessor, schedulerProvider, initialViewState) as T
    }
}
