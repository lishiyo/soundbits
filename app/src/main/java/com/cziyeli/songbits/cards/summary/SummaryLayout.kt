package com.cziyeli.songbits.cards.summary

import android.app.Activity
import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.LinearLayout
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
import com.cziyeli.songbits.base.RoundedCornerButton
import com.cziyeli.songbits.cards.CardsActivity.Companion.REQUEST_CODE_CREATE
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.cziyeli.songbits.root.RootActivity
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
        summary_tracks_recycler_view.addOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)

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
                    viewModel.currentViewState.playlist?.id
            ))
        }

        // create playlist from likes
        action_create_playlist.setOnClickListener {
            callingActivity.startActivityForResult(
                    PlaylistCardCreateActivity.create(context, viewModel.currentViewState.currentLikes), REQUEST_CODE_CREATE
            )
        }

        // === DEBUGGING ===
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

        summary_title.text = resources.getString(R.string.summary_title)
                .format(state.currentLikes.size, state.currentDislikes.size)

        when {
            state.lastResult is SummaryResult.SetTracks -> {
                adapter.setTracksAndNotify(state.allTracks)
            }
            state.lastResult is SummaryResult.SaveTracks -> { renderButton(state,
                    action_save_to_database, R.string.save_success_cta, R.string.action_error,
                    OnClickListener {
                        callingActivity.startActivity(RootActivity.create(callingActivity, RootActivity.Tab.STASH))
                    }
                )
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is SummaryResult.ChangeTrackPref -> {
                val track = (state.lastResult as? SummaryResult.ChangeTrackPref)?.track
                adapter.updateTrack(track, false)
            }
            state.lastResult is SummaryResult.PlaylistCreated -> {
                renderButton(state,
                        action_create_playlist, R.string.create_success_cta, R.string.action_error,
                        OnClickListener {
                            callingActivity.startActivity(RootActivity.create(callingActivity, RootActivity.Tab.HOME))
                        }
                )
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is SummaryResult.FetchLikedStats -> {
                progress_stats.smoothToHide()
                state.likedStats?.let {
                    stats_container_likes_first.loadTrackStats(it)
                }
                Utils.setVisible(stats_container_first, true)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is SummaryResult.FetchDislikedStats -> {
                progress_stats.smoothToHide()
                state.dislikedStats?.let {
                    stats_container_dislikes_first.loadTrackStats(it)
                }
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

        toggleButton(state, action_create_playlist as RoundedCornerButton)
    }

    private fun toggleButton(state: SummaryViewState, button: RoundedCornerButton) {
        if (state.currentLikes.isEmpty()) {
            // disable the create button
            button.isEnabled = false
            button.setColor(R.color.colorGrey)
            button.setTextColor(resources.getColor(R.color.colorBlack))
            button.setText(R.string.create_disabled)
        } else {
            // enable it
            button.isEnabled = true
            button.setColor(R.color.quartet_blue)
            button.setText(R.string.summary_create_playlist)
            button.setTextColor(resources.getColor(R.color.colorWhite))
        }
    }

    /**
     * Trigger the playlist-created result to the view model.
     */
    fun notifyPlaylistCreated() {
        simpleResultsPublisher.accept(SummaryResult.PlaylistCreated())
    }

    override fun onLiked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.LIKED)
        simpleResultsPublisher.accept(SummaryResult.ChangeTrackPref(newModel))
    }

    override fun onDisliked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.DISLIKED)
        simpleResultsPublisher.accept(SummaryResult.ChangeTrackPref(newModel))
    }

    private fun renderButton(state: SummaryViewState,
                             button: RoundedCornerButton,
                             @StringRes successResId: Int,
                             @StringRes errorResId: Int,
                             newClickListener: OnClickListener? = null
    ) {
        when {
            state.status == MviViewState.Status.LOADING -> {
                button.isEnabled = false
                button.alpha = 0.5f
            }
            state.status == MviViewState.Status.SUCCESS -> {
                button.alpha = 1f
                button.isEnabled = true
                button.text = resources.getString(successResId)
                button.setColor(R.color.venice_verde)
                button.setOnClickListener(newClickListener)
            }
            state.status == MviViewState.Status.ERROR -> {
                button.alpha = 1f
                button.isEnabled = true
                action_save_to_database.text = resources.getString(errorResId)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        summary_tracks_recycler_view.removeOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
    }

}