package com.cziyeli.songbits.cards

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewStub
import com.cziyeli.commons.Utils
import com.cziyeli.commons.toast
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.stats.SummaryActionProcessor
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.summary.SummaryLayout
import com.cziyeli.songbits.cards.summary.SummaryViewModel
import com.cziyeli.songbits.cards.summary.SummaryViewState
import com.mindorks.placeholderview.SwipeDecor
import com.mindorks.placeholderview.SwipePlaceHolderView
import com.mindorks.placeholderview.SwipeViewBuilder
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_cards.*
import lishiyo.kotlin_arch.mvibase.MviView
import lishiyo.kotlin_arch.mvibase.MviViewState
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import org.jetbrains.anko.intentFor
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by connieli on 1/1/18.
 */
class CardsActivity : AppCompatActivity(), MviView<TrackIntent, TrackViewState>, TrackCardView.TrackListener {

    companion object {
        const val PLAYLIST = "playlist"
        const val TAG: String = "CardsActivity"

        fun create(context: Context, playlist: Playlist) : Intent {
            return context.intentFor<CardsActivity>("playlist" to playlist)
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var summaryActionProcessor : SummaryActionProcessor
    // player
    @Inject @field:Named("Native") lateinit var mPlayer: PlayerInterface
    val schedulerProvider = SchedulerProvider

    // view models
    private lateinit var viewModel: CardsViewModel

    // intents
    private val mLoadPublisher = PublishSubject.create<TrackIntent.ScreenOpened>()
    private val mPlayerPublisher: PublishSubject<TrackIntent.CommandPlayer> by lazy {
        PublishSubject.create<TrackIntent.CommandPlayer>()
    }
    private val mTrackPrefPublisher : PublishSubject<TrackIntent.ChangeTrackPref> by lazy {
        PublishSubject.create<TrackIntent.ChangeTrackPref>()
    }

    private var summaryShown : Boolean = false
    private val summaryLayout: SummaryLayout by lazy {
        val stub = findViewById<ViewStub>(R.id.summary_stub)
        val view = stub.inflate() as SummaryLayout
        Utils.setVisible(view, false)
        view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cards)

        // parse out the intent bundle
        val playlist = intent.extras.get(PLAYLIST) as Playlist

        // initWith the placeholder view
        initSwipeView()

        // bind the view model after all views are done
        initViewModel(playlist)

        mLoadPublisher.onNext(TrackIntent.ScreenOpened.create(
                ownerId = playlist.owner.id,
                playlistId = playlist.id)
        )
    }

    override fun onResume() {
        super.onResume()

        // intent to load the player
        mPlayer.apply { onResume() }
    }

    override fun onPause() {
        super.onPause()

        mPlayer.apply { onPause() }
    }

    override fun onDestroy() {
        mPlayer.apply { onDestroy() }
        super.onDestroy()
    }

    private fun initSwipeView() {
        val bottomMargin = Utils.dpToPx(160)
        val windowSize = Utils.getDisplaySize(windowManager)
        val builder: SwipeViewBuilder<SwipePlaceHolderView> = swipeView.getBuilder()
        builder.setDisplayViewCount(3)
                .setIsUndoEnabled(true)
                .setHeightSwipeDistFactor(20f)
                .setWidthSwipeDistFactor(15f)
                .setSwipeDecor(SwipeDecor()
                        .setViewWidth(windowSize.x)
                        .setViewHeight(windowSize.y - bottomMargin)
                        .setViewGravity(Gravity.TOP)
                        .setPaddingTop(20)
                        .setRelativeScale(0.01f)
                        .setSwipeMaxChangeAngle(1f)
//                        .setSwipeInMsgLayoutId(R.layout.tinder_swipe_in_msg_view)
//                        .setSwipeOutMsgLayoutId(R.layout.tinder_swipe_out_msg_view)
                        .setSwipeAnimFactor(0.5f)
                        .setSwipeAnimTime(200)
                )

        discardBtn.setOnClickListener({
            swipeView.doSwipe(false)
        })

        likeBtn.setOnClickListener({
            swipeView.doSwipe(true)
        })

        undoBtn.setOnClickListener({
            swipeView.undoLastSwipe()
        })
    }

    override fun intents(): Observable<out TrackIntent> {
        return Observable.merge(
                mLoadPublisher,
                mPlayerPublisher,
                mTrackPrefPublisher
        )
    }

    override fun getPlayerIntents(): PublishSubject<TrackIntent.CommandPlayer> {
        return mPlayerPublisher
    }

    override fun getTrackIntents(): PublishSubject<TrackIntent.ChangeTrackPref> {
        return mTrackPrefPublisher
    }

    override fun render(state: TrackViewState) {
        val show = state.status == MviViewState.Status.SUCCESS && state.allTracks.isNotEmpty()
        Utils.setVisible(discardBtn, show)
        Utils.setVisible(likeBtn, show)
        Utils.setVisible(undoBtn, show)
        Utils.setVisible(swipeView, show)

        // populate if first time
        if (state.status == MviViewState.Status.SUCCESS && swipeView.childCount == 0) {
            state.allTracks.forEach {
                swipeView.addView(TrackCardView(this, it, this))
            }
        } else {
            "CardsActivity not ready: ${state.status} ".toast(this)
        }

        // todo: render play button based on mediaplayer state
        if (!summaryShown && state.reachedEnd) {
            mPlayer.apply { onDestroy() } // release the player!
            showSummary(state)
        }
    }


    // show the summary layout
    private fun showSummary(state: TrackViewState) {
        summaryShown = true

        // if we're at the end, hide cards and show summary
        (swipeView.parent as ViewGroup).removeView(swipeView)
        (buttons_row.parent as ViewGroup).removeView(buttons_row)

        Utils.setVisible(summaryLayout, true)

        // create the layout with the initial view state
//        val summaryViewModel = ViewModelProviders.of(this, viewModelFactory).get(SummaryViewModel::class.java)
        val initialState = SummaryViewState.create(state)
        val summaryViewModel = SummaryViewModel(summaryActionProcessor, schedulerProvider, initialState)
        summaryLayout.initWith(this, summaryViewModel, initialState)
    }

    private fun initViewModel(playlist: Playlist) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CardsViewModel::class.java)
        viewModel.setUp(playlist)

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(this, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
    }

}