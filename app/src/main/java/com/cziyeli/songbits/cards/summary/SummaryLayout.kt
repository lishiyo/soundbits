package com.cziyeli.songbits.cards.summary

import android.app.Activity
import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.commons.toast
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.summary.SummaryResultMarker
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.layout_summary.view.*
import kotlinx.android.synthetic.main.widget_quickcounts_row.view.*

/**
 * View to show the summary at the end of swiping
 *  - pending likes and actions
 *
 * Created by connieli on 1/7/18.
 */
class SummaryLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), MviView<SummaryIntent, SummaryViewState>,
        TracksRecyclerViewDelegate.ActionButtonListener {
    private val TAG = SummaryLayout::class.simpleName

    // view models
    private lateinit var viewModel: SummaryViewModel
    private val eventsPublisher = PublishRelay.create<SummaryIntent>()
    // stream to pipe in basic results (skips intent -> action processing)
    private val simpleResultsPublisher: PublishRelay<SummaryResultMarker> = PublishRelay.create<SummaryResultMarker>()
    private val compositeDisposable = CompositeDisposable()

    private lateinit var callingActivity: Activity
    private lateinit var adapter: TrackRowsAdapter
    private lateinit var tracksRecyclerViewDelegate: TracksRecyclerViewDelegate

    // create with initial state from previous screen
    fun initWith(activity: Activity, viewModelFromActivity: SummaryViewModel) {
        setClickListeners()
        callingActivity = activity

        // Bind the view model
        viewModel = viewModelFromActivity
        // Bind ViewModel to this view's intents stream
        viewModel.processIntents(intents())
        // Bind ViewModel to simple results stream
        viewModel.processSimpleResults(simpleResultsPublisher)
        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // set up tracks list (we already have tracks list, so don't need to do it in render)
        tracksRecyclerViewDelegate = TracksRecyclerViewDelegate(activity, summary_tracks_recycler_view, this)
        adapter = TrackRowsAdapter(context, mutableListOf())
        summary_tracks_recycler_view.adapter = adapter
        summary_tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        summary_tracks_recycler_view.disableTouchTheft()
        simpleResultsPublisher.accept(SummaryResult.SetTracks()) // command to load the tracks

        // immediately fetch stats
        eventsPublisher.accept(SummaryIntent.FetchFullStats(Repository.Pref.LIKED))
        eventsPublisher.accept(SummaryIntent.FetchFullStats(Repository.Pref.DISLIKED))
    }

    private fun setClickListeners() {
        // save all tracks with their prefs
        action_save_to_database.setOnClickListener {
            eventsPublisher.accept(SummaryIntent.SaveAllTracks(
                    viewModel.currentViewState.allTracks,
                    viewModel.currentViewState.playlist.id
            ))
        }

        // create playlist from likes
        action_create_playlist.setOnClickListener {
            callingActivity.startActivity(
                    PlaylistCardCreateActivity.create(context, viewModel.currentViewState.currentLikes)
            )
        }

        // debugging
        nuke.setOnClickListener {
            App.nukeDatabase()
            "NUKED!".toast(context)
        }
    }

    override fun intents(): Observable<out SummaryIntent> {
        return eventsPublisher
    }

    override fun render(state: SummaryViewState) {
        Utils.mLog(TAG, "render", "state", "$state")

        summary_title.text = resources.getString(R.string.summary_title).format(state.currentLikes.size, state.currentDislikes.size) // for

        when {
            state.lastResult is SummaryResult.SetTracks -> {
                Utils.mLog(TAG, " got set tracks! loading...")
                adapter.setTracksAndNotify(state.allTracks)
            }
            state.lastResult is SummaryResult.SaveTracks -> {
                renderButton(state, action_save_to_database, R.string.save_success, R.string.action_error)
            }
            state.lastResult is SummaryResult.CreatePlaylistWithTracks -> {
                // todo actually implement this
                renderButton(state, action_create_playlist, R.string.save_success, R.string.action_error)
            }
            state.status == MviViewState.Status.SUCCESS && state.likedStats != null && state.dislikedStats != null -> {
                progress_stats.smoothToHide()
                stats_container_likes_first.loadTrackStats(state.likedStats)
                stats_container_dislikes_first.loadTrackStats(state.dislikedStats)
                Utils.setVisible(stats_container_first, true)
            }
            state.status == MviViewState.Status.ERROR -> {
                "error in rendering: ${state.error?.localizedMessage}".toast(context)
            }
            else -> {
                Utils.setVisible(stats_container_first, false)
                progress_stats.smoothToShow()
            }
        }

        if (state.status == MviViewState.Status.SUCCESS) {
            quickstats_likes.text = "${state.currentLikes.size} likes"
            quickstats_dislikes.text = "${state.currentDislikes.size} dislikes"
            quickstats_total.text = "${state.allTracks.size} swiped"
        }
    }

    override fun onLiked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.LIKED)
        // modify it in viewmodel
//        eventsPublisher.accept(CardsIntent.ChangeTrackPref.like(newModel))
    }

    override fun onDisliked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.DISLIKED)
        // modify it in viewmodel
//        eventsPublisher.accept(CardsIntent.ChangeTrackPref.dislike(newModel))
    }

    private fun renderButton(state: SummaryViewState, button: TextView, @StringRes successResId: Int, @StringRes errorResId: Int) {
        when {
            state.status == MviViewState.Status.LOADING -> {
                button.isEnabled = false
                button.alpha = 0.5f
            }
            state.status == MviViewState.Status.SUCCESS -> {
                button.alpha = 1f
                button.isEnabled = false
                button.text = resources.getString(successResId)
            }
            state.status == MviViewState.Status.ERROR -> {
                button.alpha = 1f
                button.isEnabled = true
                action_save_to_database.text = resources.getString(errorResId)
            }
        }
    }

}