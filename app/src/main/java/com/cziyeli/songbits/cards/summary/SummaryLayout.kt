package com.cziyeli.songbits.cards.summary

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.commons.toast
import com.cziyeli.data.Repository
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
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
) : LinearLayout(context, attrs, defStyleAttr), MviView<SummaryIntent, SummaryViewState> {
    private val TAG = SummaryLayout::class.simpleName

    // view models
    private lateinit var viewModel: SummaryViewModel

    private val eventsPublisher = PublishRelay.create<SummaryIntent>()
    private lateinit var callingActivity: Activity
    private val compositeDisposable = CompositeDisposable()
    
    // create with initial state from previous screen
    fun initWith(activity: Activity, viewModelFromActivity: SummaryViewModel) {
        setClickListeners()

        callingActivity = activity
        // Bind the view model
        viewModel = viewModelFromActivity
        // Bind ViewModel to this view's intents stream
        viewModel.processIntents(intents())
        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // immediately fetch stats
        eventsPublisher.accept(SummaryIntent.FetchFullStats(viewModel.currentViewState.currentLikes, Repository.Pref.LIKED))
        eventsPublisher.accept(SummaryIntent.FetchFullStats(viewModel.currentViewState.currentDislikes, Repository.Pref.DISLIKED))
    }

    private fun setClickListeners() {
        // init click listeners
        action_save_to_database.setOnClickListener {
            eventsPublisher.accept(SummaryIntent.SaveAllTracks(
                    viewModel.currentViewState.allTracks,
                    viewModel.currentViewState.playlist.id
            ))
        }

        action_create_playlist.setOnClickListener {
            callingActivity.startActivity(
                    PlaylistCardCreateActivity.create(context, viewModel.currentViewState.currentLikes)
            )
        }

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
            quickstats_likes.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_dislikes.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_total.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_likes.text = "${state.currentLikes.size} likes"
            quickstats_dislikes.text = "${state.currentDislikes.size} dislikes"
            quickstats_total.text = "${state.allTracks.size} swiped"
        }
    }

}