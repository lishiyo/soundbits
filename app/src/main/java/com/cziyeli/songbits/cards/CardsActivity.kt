package com.cziyeli.songbits.cards

import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowManager
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.summary.SummaryLayout
import com.cziyeli.songbits.cards.summary.SummaryViewModel
import com.cziyeli.songbits.cards.summary.SummaryViewState
import com.mindorks.placeholderview.SwipeDecor
import com.mindorks.placeholderview.SwipePlaceHolderView
import com.mindorks.placeholderview.SwipeViewBuilder
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_cards.*
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.intentFor
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by connieli on 1/1/18.
 */
class CardsActivity : AppCompatActivity(), MviView<CardsIntent, TrackViewState>, TrackCardView.TrackListener {

    companion object {
        // open Create activity with request code
        const val REQUEST_CODE_CREATE = 99
        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EXTRA_TRACKS_TO_SWIPE = "key_tracks_to_swipe"
        const val TAG: String = "CardsActivity"

        fun create(context: Context, playlist: Playlist, tracksToSwipe: List<TrackModel>? = null) : Intent {
            return context.intentFor<CardsActivity>(EXTRA_PLAYLIST to playlist, EXTRA_TRACKS_TO_SWIPE to tracksToSwipe)
        }
    }

    @Inject lateinit var summaryActionProcessor : SummaryActionProcessor
    @Inject @field:Named("Native") lateinit var mPlayer: PlayerInterface
    val schedulerProvider = SchedulerProvider

    // view models
    @Inject @field:Named("CardsViewModel") lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CardsViewModel

    // intents
    private val mLoadPublisher = PublishSubject.create<CardsIntent>()
    private val mPlayerPublisher: PublishSubject<CardsIntent.CommandPlayer> by lazy {
        PublishSubject.create<CardsIntent.CommandPlayer>()
    }
    private val mCardsPrefPublisher: PublishSubject<CardsIntent.ChangeTrackPref> by lazy {
        PublishSubject.create<CardsIntent.ChangeTrackPref>()
    }

    lateinit var playlist: Playlist

    private var summaryShown : Boolean = false // TODO move into viewmodel
    private val summaryLayout: SummaryLayout by lazy {
        val stub = findViewById<ViewStub>(R.id.summary_stub)
        val view = stub.inflate() as SummaryLayout
        Utils.setVisible(view, false)
        view
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_cards)

        // parse out the intent bundle
        playlist = intent.extras.get(EXTRA_PLAYLIST) as Playlist
        val tracksToSwipe = intent.extras.get(EXTRA_TRACKS_TO_SWIPE) as List<TrackModel>?

        AndroidInjection.inject(this)

        // initWith the placeholder view
        initSwipeView()

        // bind the view model after all views are done
        initViewModel(playlist)

        // fetch track cards
        if (tracksToSwipe != null && tracksToSwipe.isNotEmpty()) {
            // tracks were passed - just use these
            Utils.mLog(TAG, "got tracksToSwipe: ${tracksToSwipe.size}")
            mLoadPublisher.onNext(CardsIntent.ScreenOpenedWithTracks(playlist, tracksToSwipe))
        } else {
            // no tracks passed - fetch all from remote
            Utils.mLog(TAG, "no tracksToSwipe, fetching from remote")
            mLoadPublisher.onNext(CardsIntent.ScreenOpenedNoTracks.create(
                    ownerId = playlist.owner.id,
                    playlistId = playlist.id)
            )
        }
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
//                        .setPaddingLeft(20)
//                        .setPaddingTop(10)
                        .setSwipeRotationAngle(10)
                        .setRelativeScale(0.01f)
                        .setSwipeMaxChangeAngle(1f)
                        .setSwipeInMsgLayoutId(R.layout.tinder_swipe_in_msg_view)
                        .setSwipeOutMsgLayoutId(R.layout.tinder_swipe_out_msg_view)
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

    override fun intents(): Observable<out CardsIntent> {
        return Observable.merge(
                mLoadPublisher,
                mPlayerPublisher,
                mCardsPrefPublisher
        )
    }

    override fun getPlayerIntents(): PublishSubject<CardsIntent.CommandPlayer> {
        return mPlayerPublisher
    }

    override fun getTrackIntents(): PublishSubject<CardsIntent.ChangeTrackPref> {
        return mCardsPrefPublisher
    }

    override fun render(state: TrackViewState) {
        val show = state.isSuccess() && state.allTracks.isNotEmpty()
        Utils.setVisible(discardBtn, show)
        Utils.setVisible(likeBtn, show)
        Utils.setVisible(undoBtn, show)
        Utils.setVisible(swipeView, show)

        Utils.mLog(TAG, "render!", "state", state.toString())

        // populate deck if first time
        if (state.status == TrackViewState.TracksLoadedStatus.SUCCESS && swipeView.childCount == 0) {
            state.allTracks.forEachWithIndex { position, model ->
                swipeView.addView(TrackCardView(this, model, this))
            }
        }

        // todo: render play button based on mediaplayer state
        if (!summaryShown && state.reachedEnd) {
            mPlayer.run { onDestroy() } // release the player!
            showSummary(state)
        }
    }

    // show the summary layout
    private fun showSummary(state: TrackViewState) {
        summaryShown = true

        // if we're at the end, hide cards and show summary
        swipeView.removeAllViews()
        (swipeView.parent as ViewGroup).removeView(swipeView)
        (buttons_row.parent as ViewGroup).removeView(buttons_row)
        Utils.setVisible(summaryLayout, true)

        // create the layout with the initial view state
        val initialState = SummaryViewState.create(state)
        val summaryViewModel = SummaryViewModel(summaryActionProcessor, schedulerProvider, initialState)
        summaryLayout.initWith(this, summaryViewModel)
    }

    private fun initViewModel(playlist: Playlist) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(CardsViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE_CREATE && resultCode == Activity.RESULT_OK) {
            summaryLayout.notifyPlaylistCreated()
        }
    }

    override fun onDestroy() {
        mPlayer.apply { onDestroy() }
        compositeDisposable.clear()
        super.onDestroy()
    }
}