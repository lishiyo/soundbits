package com.cziyeli.songbits.playlistcard.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.view.View
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlistcard.PlaylistCardCreateActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateViewModel
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import dagger.Module
import dagger.Provides
import kotlinx.android.synthetic.main.widget_playlist_card_create.*
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named

/**
 * Created by connieli on 1/1/18.
 */
@Module
class PlaylistCardCreateModule {

    @Provides
    @PerActivity
    fun provideTracksRecyclerViewDelegate(@Named("ActivityContext") activity: PlaylistCardCreateActivity): TracksRecyclerViewDelegate {
        val onTouchListener = RecyclerTouchListener(activity, activity.create_tracks_recycler_view)
        onTouchListener
                .setViewsToFade(R.id.track_status)
                .setSwipeable(false) // Create is read-only!

        return TracksRecyclerViewDelegate(activity, activity.create_tracks_recycler_view, activity, onSwipeListener = object :
                RecyclerTouchListener.OnSwipeListener {
            override fun onSwipeOptionsClosed(foregroundView: View?, backgroundView: View?) {
                // no-op
            }

            override fun onSwipeOptionsOpened(foregroundView: View?, backgroundView: View?) {
                // no-op
            }

            override fun onForegroundAnimationStart(isFgOpening: Boolean, duration: Long, foregroundView: View?, backgroundView: View?) {
                // no-op
            }

        }, onTouchListener = onTouchListener)
    }

    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(activity: PlaylistCardCreateActivity): PlaylistCardCreateActivity {
        return activity
    }

    @Provides
    @PerActivity
    fun provideInitialPendingTracks(activity: PlaylistCardCreateActivity): List<TrackModel> {
        return activity.initialPendingTracks // call inject after binding this in the activity!
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
