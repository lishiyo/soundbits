package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.Observer
import android.content.Context
import android.support.v4.app.FragmentActivity
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cziyeli.songbits.R
import com.wang.avi.AVLoadingIndicatorView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
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

    // view models
    private lateinit var viewModel: SummaryViewModel

    // fire off intents
    private val mStatsPublisher = PublishSubject.create<SummaryIntent.LoadStats>()

    // lifecycle/viewmodel owner of this layout
    private lateinit var mOwnerActivity: FragmentActivity

    // views
    lateinit var rootView: ViewGroup
    lateinit var titleView: TextView
    lateinit var progressView: AVLoadingIndicatorView

    // create with initial state from previous screen
    fun initWith(context: FragmentActivity, initialState: SummaryViewState, viewModelFromActivity: SummaryViewModel) {
        // setup views
        mOwnerActivity = context
        rootView = inflate(context, R.layout.layout_summary, this) as ViewGroup
        titleView = findViewById(R.id.title)
        progressView = findViewById(R.id.progress)

        // bind the view model
        viewModel = viewModelFromActivity

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { context.lifecycle.addObserver(it) }
        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(mOwnerActivity, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
        viewModel.setUp(initialState) // should trigger render with initial state

        // immediately fetch with the like ids
        mStatsPublisher.onNext(SummaryIntent.LoadStats.create(initialState.currentLikeIds))
    }

    override fun intents(): Observable<out SummaryIntent> {
        return mStatsPublisher
    }

    override fun render(state: SummaryViewState) {
        val title = "Liked ${state.currentLikes.size} and disliked ${state.currentDislikes.size} " +
                "out of ${state.allTracks.size} tracks!"
        titleView.text = title // for debugging

        if (state.status == MviViewState.Status.SUCCESS) {
            progressView.smoothToHide()
        } else {
            progressView.smoothToShow()
        }

        // TODO: get the tracks stats
        // when that comes back, hide the progressView and show the track stats/card

    }

}