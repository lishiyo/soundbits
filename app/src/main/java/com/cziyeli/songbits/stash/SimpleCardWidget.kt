package com.cziyeli.songbits.stash

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.cziyeli.songbits.playlistcard.PlaylistCardWidget
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.jakewharton.rxrelay2.PublishRelay
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.widget_simple_card.view.*
import java.util.*

/**
 * Very similar to [PlaylistCardWidget], but doesn't need to be instantiated with a playlist
 * - just any list of tracks.
 */
class SimpleCardWidget : NestedScrollView, MviView<CardIntentMarker, SimpleCardViewModel.ViewState> {
    val TAG = SimpleCardWidget::class.simpleName

    // backing viewmodel for this card
    lateinit var viewModel: SimpleCardViewModel
    private val compositeDisposable = CompositeDisposable()

    // intents
    private val eventsPublisher = PublishRelay.create<CardIntentMarker>()
    // stream to pipe in basic results (skips intent -> action processing)
    private val simpleResultsPublisher: PublishRelay<CardResultMarker> = PublishRelay.create<CardResultMarker>()

    // Listener for the track rows
    private lateinit var adapter: TrackRowsAdapter
    private var onTouchListener: RecyclerTouchListener? = null
    private var onSwipeListener: RecyclerTouchListener.OnSwipeListener? = null

    private var carouselImageSet: Boolean = false
    private var carouselHeaderUrl: String? = null

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_simple_card, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    // passed from StashFragment
    fun loadTracks(tracks: List<TrackModel>) {
        Utils.mLog(TAG, "loadTracks", "${tracks.size}")
        adapter.setTracksAndNotify(tracks, !card_expansion_layout.isExpanded)

        if (tracks.isNotEmpty()) {
            // update the title
            card_expansion_header_title.text = resources.getString(R.string.expand_tracks).format(tracks.size)
            Utils.setVisible(card_header_indicator, true)

            // fetch the track stats of these tracks
            eventsPublisher.accept(StatsIntent.FetchFullStats(tracks))


        }

        // set the carousel header
        carouselHeaderUrl = when {
            viewModel.currentViewState.carouselHeaderUrl != null -> viewModel.currentViewState.carouselHeaderUrl
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

    /**
     * Container fragment initializes the widget.
     */
    fun initWith(title: String,
                 tracks: List<TrackModel>,
                 swipeListener: RecyclerTouchListener.OnSwipeListener? = null,
                 touchListener: RecyclerTouchListener? = null,
                 initialViewModel: SimpleCardViewModel
    ) {
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

        onSwipeListener = swipeListener
        onTouchListener = touchListener

        // set header
        card_title.text = title

        // set up tracks list (don't need to re-render)
        adapter = TrackRowsAdapter(context, tracks.toMutableList())
        card_tracks_recycler_view.adapter = adapter
        card_tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        card_tracks_recycler_view.disableTouchTheft()
    }

    override fun intents(): Observable<out CardIntentMarker> {
        return eventsPublisher
    }

    override fun render(state: SimpleCardViewModel.ViewState) {
        Utils.mLog(TAG, "RENDER", "$state")

        card_fab_text.text = "${state.tracks.size}"

        if (state.carouselHeaderUrl != null && !carouselImageSet) {
            setCarousel(state) // set the header image (once)
        }

        if (state.isFetchStatsSuccess()) {
            stats_container_left.loadTrackStats(state.trackStats!!)
            stats_container_right.loadTrackStats(state.trackStats, true)
        }
    }

    private fun setCarousel(state: SimpleCardViewModel.ViewState) {
        if (state.carouselHeaderUrl == null || carouselImageSet) {
            return
        }

        Glide.with(this)
                .load(state.carouselHeaderUrl)
                .into(card_image_background)

        carouselImageSet = true
    }
}
