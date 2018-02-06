package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.Observer
import android.content.Context
import android.support.v4.app.FragmentActivity
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.commons.toast
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
import com.wang.avi.AVLoadingIndicatorView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_summary.view.*


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

    // fire off intents
    private val mStatsPublisher = PublishSubject.create<SummaryIntent.FetchStats>()
    private val mUserSavePublisher = PublishSubject.create<SummaryIntent.SaveAllTracks>()
    private val mCreatePlaylistPublisher = PublishSubject.create<SummaryIntent.CreatePlaylistWithTracks>()

    // views
    lateinit var rootView: ViewGroup
    lateinit var titleView: TextView
    lateinit var progressView: AVLoadingIndicatorView

    // create with initial state from previous screen
    fun initWith(context: FragmentActivity, viewModelFromActivity: SummaryViewModel) {
        // setup views
        rootView = inflate(context, R.layout.layout_summary, this) as ViewGroup
        titleView = findViewById(R.id.title)
        progressView = findViewById(R.id.progress)

        // Bind the view model
        viewModel = viewModelFromActivity
        // Bind ViewModel to this view's intents stream
        viewModel.processIntents(intents())
        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(context, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // immediately fetch stats of the like ids
        val initialViewState = viewModel.states().value
        mStatsPublisher.onNext(SummaryIntent.FetchStats(initialViewState!!.trackIdsForStats()))

        // init click listeners
        action_save_to_database.setOnClickListener {
            mUserSavePublisher.onNext(SummaryIntent.SaveAllTracks(initialViewState.allTracks, initialViewState.playlist!!.id))
        }

        action_create_playlist.setOnClickListener {
            // just testing
            mCreatePlaylistPublisher.onNext(SummaryIntent.CreatePlaylistWithTracks(
                    ownerId = App.getCurrentUserId(),
                    name = "songbits test",
                    description = "a little test",
                    public = false,
                    tracks = initialViewState.currentLikes)
            )
        }

        nuke.setOnClickListener {
            App.nukeDatabase()
            "NUKED!".toast(context)
        }
    }

    override fun intents(): Observable<out SummaryIntent> {
        return Observable.merge(
                mStatsPublisher,
                mUserSavePublisher,
                mCreatePlaylistPublisher
        )
    }

    override fun render(state: SummaryViewState) {
        val title = "Liked ${state.currentLikes.size} and disliked ${state.currentDislikes.size} " +
                "out of ${state.allTracks.size} tracks!"
        titleView.text = title // for debugging

        Utils.setVisible(stats_container, true)

        when (state.status) {
            MviViewState.Status.SUCCESS -> {
                progressView.smoothToHide()
                stats_summary.text = "stats: ${state.stats?.toString()}"
            }
            MviViewState.Status.ERROR -> {
                "error in rendering: ${state.error?.localizedMessage}".toast(context)
            }
            else -> progressView.smoothToShow()
        }
    }

}