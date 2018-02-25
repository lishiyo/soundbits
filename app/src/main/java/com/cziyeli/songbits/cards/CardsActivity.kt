package com.cziyeli.songbits.cards

import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
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
import com.jakewharton.rxrelay2.PublishRelay
import com.mindorks.placeholderview.SwipeDecor
import com.mindorks.placeholderview.SwipePlaceHolderView
import com.mindorks.placeholderview.SwipeViewBuilder
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
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

        fun create(context: Context, playlist: Playlist?, tracksToSwipe: List<TrackModel>? = null) : Intent {
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
    private val mLoadPublisher: PublishRelay<CardsIntent> by lazy {
        PublishRelay.create<CardsIntent>()
    }
    private val mPlayerPublisher: PublishRelay<CardsIntent.CommandPlayer> by lazy {
        PublishRelay.create<CardsIntent.CommandPlayer>()
    }
    private val mCardsPrefPublisher: PublishRelay<CardsIntent.ChangeTrackPref> by lazy {
        PublishRelay.create<CardsIntent.ChangeTrackPref>()
    }

    var playlist: Playlist? = null

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

        // set full screen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_cards)

        // parse out the intent bundle
        playlist = intent.extras.get(EXTRA_PLAYLIST) as Playlist?
        val tracksToSwipe = intent.extras.get(EXTRA_TRACKS_TO_SWIPE) as List<TrackModel>?

        AndroidInjection.inject(this)

        // initWith the placeholder view
        initSwipeView()

        // bind the view model after all views are done
        initViewModel()

        // fetch track cards
        if (tracksToSwipe != null && tracksToSwipe.isNotEmpty()) {
            // tracks were passed - just use these
            mLoadPublisher.accept(CardsIntent.ScreenOpenedWithTracks(playlist, tracksToSwipe))
        } else {
            // no tracks passed - fetch all from remote
            mLoadPublisher.accept(CardsIntent.ScreenOpenedNoTracks.create(
                    ownerId = playlist!!.owner.id,
                    playlistId = playlist!!.id)
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
        val builder: SwipeViewBuilder<SwipePlaceHolderView> = swipe_view.getBuilder()
        builder.setDisplayViewCount(3)
                .setIsUndoEnabled(true)
                .setHeightSwipeDistFactor(20f)
                .setWidthSwipeDistFactor(15f)
                .setSwipeDecor(SwipeDecor()
//                        .setViewWidth(windowSize.x)
//                        .setViewHeight(windowSize.y - bottomMargin)
//                        .setMarginTop(30)
//                        .setMarginLeft(30)
//                        .setViewGravity(Gravity.TOP)
//                        .setPaddingLeft(20)
                        .setPaddingTop(10)
                        .setSwipeRotationAngle(10)
                        .setRelativeScale(0.01f)
                        .setSwipeMaxChangeAngle(1f)
                        .setSwipeInMsgLayoutId(R.layout.tinder_swipe_in_msg_view)
                        .setSwipeOutMsgLayoutId(R.layout.tinder_swipe_out_msg_view)
                        .setSwipeAnimFactor(0.5f)
                        .setSwipeAnimTime(200)
                )

    }

    override fun intents(): Observable<out CardsIntent> {
        return Observable.merge(
                mLoadPublisher,
                mPlayerPublisher,
                mCardsPrefPublisher
        )
    }

    override fun getPlayerIntents(): PublishRelay<CardsIntent.CommandPlayer> {
        return mPlayerPublisher
    }

    override fun onChangePref(model: TrackModel, pref: TrackModel.Pref) {
        when (pref) {
            TrackModel.Pref.LIKED -> {
                mCardsPrefPublisher.accept(CardsIntent.ChangeTrackPref.like(model))
            }
            TrackModel.Pref.DISLIKED -> {
                mCardsPrefPublisher.accept(CardsIntent.ChangeTrackPref.dislike(model))
            }
            TrackModel.Pref.UNSEEN -> {
                mCardsPrefPublisher.accept(CardsIntent.ChangeTrackPref.clear(model))
            }
        }
    }

    override fun doSwipe(pref: TrackModel.Pref) {
        when (pref) {
            TrackModel.Pref.LIKED -> {
                swipe_view.doSwipe(true)
            }
            TrackModel.Pref.DISLIKED -> {
                swipe_view.doSwipe(false)
            }
            TrackModel.Pref.UNSEEN -> {
                swipe_view.undoLastSwipe()
            }
        }
    }

    override fun render(state: TrackViewState) {

        Utils.mLog(TAG, "render!", "state", state.toString())

        // populate deck if first time
        if (state.status == TrackViewState.TracksLoadedStatus.SUCCESS && swipe_view.childCount == 0) {
            state.allTracks.forEachWithIndex { position, model ->
                swipe_view.addView(TrackCardView(this, model, this))
            }
        }

        if (!summaryShown && state.reachedEnd) {
            mPlayer.run { onDestroy() } // release the player!
            showSummary(state)
        }
    }

    // show the summary layout
    private fun showSummary(state: TrackViewState) {
        summaryShown = true

        // if we're at the end, hide cards and show summary
        swipe_view.removeAllViews()
        (swipe_view.parent as ViewGroup).removeView(swipe_view)
        Utils.setVisible(summaryLayout, true)

        // create the layout with the initial view state
        val initialState = SummaryViewState.create(state)
        val summaryViewModel = SummaryViewModel(summaryActionProcessor, schedulerProvider, initialState)
        summaryLayout.initWith(this, summaryViewModel)
    }

    private fun initViewModel() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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