package com.cziyeli.songbits.stash

import android.app.Activity
import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.CardsIntent
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.cards.summary.SummaryIntent
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.cziyeli.songbits.playlistcard.PlaylistCardWidget
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateWidget.Companion.FAB_CREATE_COLOR_0
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateWidget.Companion.FAB_CREATE_COLOR_1
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateWidget.Companion.FAB_CREATE_COLOR_2
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateWidget.Companion.FAB_CREATE_COLOR_3
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.saeid.fabloading.LoadingView
import kotlinx.android.synthetic.main.widget_expandable_tracks.view.*
import kotlinx.android.synthetic.main.widget_simple_card.view.*
import java.util.*



/**
 * Very similar to [PlaylistCardWidget], but doesn't need to be instantiated with a playlist
 * - just any list of tracks.
 */
class SimpleCardWidget : NestedScrollView, MviView<CardIntentMarker,
        SimpleCardViewModel.ViewState>,
        TracksRecyclerViewDelegate.ActionButtonListener
{
    val TAG = SimpleCardWidget::class.simpleName
    companion object {
        const val HEADER_DIM: Float = 0.4f
        const val HEADER_DIM_DARK: Float = 0.7f
    }


    // backing viewmodel for this card
    lateinit var viewModel: SimpleCardViewModel

    // intents
    private val eventsPublisher = PublishRelay.create<CardIntentMarker>()
    // stream to pipe in basic results (skips intent -> action processing)
    private val simpleResultsPublisher: PublishRelay<CardResultMarker> = PublishRelay.create<CardResultMarker>()
    private val compositeDisposable = CompositeDisposable()

    // Views
    private lateinit var activity: Activity
    private var carouselHeaderUrl: String? = null

    private lateinit var onClearedListener: StashFragment.OnCleared
    // Listener for the FAB menu
    private val onFabSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_clear -> {
                onClearedListener.onCleared()
            }
            R.id.menu_create_playlist -> {
                if (viewModel.pendingTracks.isEmpty()) {
                    "no liked tracks yet! swipe first?".toast(context)
                } else {
                    enableCreateTitle(true)
                    // switch fab icon
                    Utils.setVisible(card_fab, false)
                    Utils.setVisible(card_fab_create, true)
                }
            }
        }
    }
    private lateinit var adapter: TrackRowsAdapter
    private lateinit var tracksRecyclerViewDelegate: TracksRecyclerViewDelegate

    // Flag for whether we've created the new playlist or not
    private var isFinished: Boolean = false

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_simple_card, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        setOnTouchListener { v, event ->
            if (card_fab_menu.isShowing) {
                card_fab_menu.closeMenu()
            }
            false
        }

        // delegate ui events to the mvi view
        card_fab_create.setOnClickListener { _ ->
            createPlaylist(App.getCurrentUserId(), viewModel.pendingTracks)
        }

        enableCreateTitle(false)
    }

    /**
     * Container fragment initializes the widget.
     */
    fun initWith(title: String,
                 tracks: List<TrackModel>,
                 activity: Activity,
                 initialViewModel: SimpleCardViewModel,
                 onClearedListener: StashFragment.OnCleared
    ) {
        this.activity = activity
        this.onClearedListener = onClearedListener

        // Bind the view model
        viewModel = initialViewModel
        // Bind ViewModel to this view's intents stream
        viewModel.processIntents(intents())
        // Bind ViewModel to thisview's simple results stream
        viewModel.processSimpleResults(simpleResultsPublisher)

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state) // pass to the widgets
                    }
                })
        )

        tracksRecyclerViewDelegate = TracksRecyclerViewDelegate(activity, tracks_recycler_view, this)

        // set header
        card_title.setText(title)

        // bind the fab menu
        if (card_fab_menu != null && card_fab_button != null) {
            card_fab_menu!!.bindAnchorView(card_fab_button!!)
            card_fab_menu!!.setOnFABMenuSelectedListener(onFabSelectedListener)
        }

        // set up the loading fab
        card_fab_create.addAnimation(FAB_CREATE_COLOR_0, R.drawable.basic_plus_fab,
                LoadingView.FROM_LEFT)
        card_fab_create.addAnimation(FAB_CREATE_COLOR_1, R.drawable.basic_play_fab,
                LoadingView.FROM_LEFT)
        card_fab_create.addAnimation(FAB_CREATE_COLOR_2, R.drawable.basic_pause_fab,
                LoadingView.FROM_TOP)
        card_fab_create.addAnimation(FAB_CREATE_COLOR_3, R.drawable.basic_stop_1_fab,
                LoadingView.FROM_RIGHT)

        // set up tracks list (don't need to re-render)
        adapter = TrackRowsAdapter(context, tracks.toMutableList())
        tracks_recycler_view.adapter = adapter
        tracks_recycler_view.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Container fragment passes on tracks from [RootActivity]
     */
    fun loadTracks(tracks: List<TrackModel>) {
        Utils.mLog(TAG, "loadTracks: ${tracks.size}")
        simpleResultsPublisher.accept(CardResult.TracksSet(tracks))
        adapter.setTracksAndNotify(tracks, !expansion_layout.isExpanded || (tracks.isEmpty()))

        if (tracks.isNotEmpty()) {
            // update the title
            expansion_header_title.text = resources.getString(R.string.expand_tracks).format(tracks.size)
            Utils.setVisible(header_indicator, true)

            // fetch the track stats of these tracks
            eventsPublisher.accept(StatsIntent.FetchFullStats(tracks))
        } else {
            // cleared tracks, reset the title and header image
            expansion_header_title.text = resources.getString(R.string.expand_tracks_default)
            Utils.setVisible(header_indicator, false)
        }

        // set the carousel header
        carouselHeaderUrl = when {
            tracks.isEmpty() -> "" // in case we cleared
            viewModel.currentViewState.carouselHeaderUrl?.isNotBlank() == true -> viewModel.currentViewState.carouselHeaderUrl
            tracks.isNotEmpty() -> {
                val headerImageIndex = Random().nextInt(tracks.size)
                tracks[headerImageIndex].imageUrl
            }
            else -> null
        }
        carouselHeaderUrl?.let {
            simpleResultsPublisher.accept(CardResult.HeaderSet(it))
        }
    }

    override fun intents(): Observable<out CardIntentMarker> {
        return eventsPublisher
    }

    override fun render(state: SimpleCardViewModel.ViewState) {
        when {
            state.lastResult is CardResult.HeaderSet -> {
                setCarousel(state)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is TrackResult.ChangePrefResult -> {
                val track = (state.lastResult as? TrackResult.ChangePrefResult)?.currentTrack
                // remove the track completely upon change or else the indices get out of date
                if (!state.tracks.contains(track)) { // track no longer matches the others
                    adapter.removeTrack(track)
                } else {
                    adapter.updateTrack(track, false)
                }
            }
            state.isFetchStatsSuccess() -> {
                stats_container_left.loadTrackStats(state.trackStats!!)
                stats_container_right.loadTrackStats(state.trackStats)
            }
            state.isCreateFinished() && state.status == MviViewState.Status.SUCCESS -> {
                onPlaylistCreated(card_title.text.toString(), state)
                card_fab_create.pauseAnimation()
                card_fab.setImageResource(R.drawable.note_happy_colored) // switch fab back
            }
            state.isCreateLoading() -> {
                card_fab_create.resumeAnimation()
            }
            state.isError() -> "something went wrong".toast(context)
        }
    }

    override fun onLiked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.LIKED)
        eventsPublisher.accept(CardsIntent.ChangeTrackPref.like(newModel))
    }

    override fun onDisliked(position: Int) {
        val model = adapter.tracks[position]
        val newModel = model.copy(pref = TrackModel.Pref.DISLIKED)
        eventsPublisher.accept(CardsIntent.ChangeTrackPref.dislike(newModel))
    }

    // Actually create the playlist now
    private fun createPlaylist(ownerId: String, tracks: List<TrackModel>) {
        if (isFinished) {
            return
        }

        if (card_title.text.isBlank()) {
            "you have to give your playlist a name!".toast(context)
            return
        }

        card_fab_create.startAnimation()
        eventsPublisher.accept(SummaryIntent.CreatePlaylistWithTracks(
                ownerId = ownerId,
                name = card_title.text.toString(),
                description = resources.getString(R.string.new_playlist_description),
                public = false,
                tracks = tracks
        ))
    }

    private fun onPlaylistCreated(newTitle: String, state: SimpleCardViewModel.ViewState) {
        isFinished = true
        sc_card_view.cardElevation = resources.getDimension(R.dimen.playlist_card_finished_elevation)
        Utils.hideKeyboard(context, card_title)

        // disable title editing
        enableCreateTitle(false)

        isEnabled = false
        descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
    }

    private fun enableCreateTitle(enable: Boolean = true) {
        if (!enable) {
            card_title.isEnabled = false
            Utils.setVisible(dotted_line, false)
            card_image_dim_overlay.alpha = HEADER_DIM
        } else {
            Utils.setVisible(dotted_line, true)
            card_image_dim_overlay.alpha = HEADER_DIM_DARK
            card_title.isFocusable = true
            card_title.isFocusableInTouchMode = true
            card_title.isEnabled = true
            card_title.isClickable = true
            card_title.setText("")
            card_title.requestFocus()
        }
    }

    private fun setCarousel(state: SimpleCardViewModel.ViewState) {
        if (state.carouselHeaderUrl == null) {
            return
        }

        if (state.carouselHeaderUrl.isEmpty()) {
            // go back to default
            card_image_background.setImageResource(R.drawable.gradient_purples)
            Utils.setVisible(card_image_dim_overlay, false)
        } else {
            Glide.with(this)
                    .load(state.carouselHeaderUrl)
                    .into(card_image_background)
        }
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        return isFinished
    }

    fun onResume() {
        tracks_recycler_view.addOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
    }

    fun onPause() {
        tracks_recycler_view.removeOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
    }

}
