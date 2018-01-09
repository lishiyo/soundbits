package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.Observer
import android.content.Context
import android.support.v4.app.FragmentActivity
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.commons.toast
import com.cziyeli.songbits.R
import com.wang.avi.AVLoadingIndicatorView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_summary.view.*
import lishiyo.kotlin_arch.mvibase.MviView
import lishiyo.kotlin_arch.mvibase.MviViewState


/**
 * View to show the summary at the end of swiping
 *  - pending likes and actions
 *
 * Created by connieli on 1/7/18.
 */
class SummaryLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), MviView<SummaryIntent, SummaryViewState> {
    private val TAG = SummaryLayout::class.simpleName

    // view models
    private lateinit var viewModel: SummaryViewModel

    // fire off intents
    private val mStatsPublisher = PublishSubject.create<SummaryIntent.LoadStats>()

    // views
    lateinit var rootView: ViewGroup
    lateinit var titleView: TextView
    lateinit var progressView: AVLoadingIndicatorView

    // create with initial state from previous screen
    fun initWith(context: FragmentActivity, viewModelFromActivity: SummaryViewModel, initialViewState: SummaryViewState) {
        // setup views
        rootView = inflate(context, R.layout.layout_summary, this) as ViewGroup
        titleView = findViewById(R.id.title)
        progressView = findViewById(R.id.progress)

        // Bind the view model
        viewModel = viewModelFromActivity
        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())
        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { context.lifecycle.addObserver(it) }
        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(context, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // immediately fetch with the like ids
        mStatsPublisher.onNext(SummaryIntent.LoadStats.create(initialViewState.trackIdsToFetch()))
    }

    override fun intents(): Observable<out SummaryIntent> {
        return mStatsPublisher
    }

    override fun render(state: SummaryViewState) {
        val title = "Liked ${state.currentLikes.size} and disliked ${state.currentDislikes.size} " +
                "out of ${state.allTracks.size} tracks!"
        titleView.text = title // for debugging

        val showResults = state.status == MviViewState.Status.SUCCESS || state.status == MviViewState.Status.ERROR
        Utils.setVisible(stats_container, showResults)

        when (state.status) {
            MviViewState.Status.SUCCESS -> {
                progressView.smoothToHide()
                stats_summary.text = "stats: ${state.stats?.printSummary()}"
            }
            MviViewState.Status.ERROR -> {
                "error in fetching stats: ${state.error?.localizedMessage}".toast(context)
            }
            else -> progressView.smoothToShow()
        }
    }

}