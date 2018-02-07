package com.cziyeli.songbits.stash

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.toast
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
import kotlinx.android.synthetic.main.widget_simple_card.view.*

/**
 * Very similar to [PlaylistCardWidget], but doesn't need to be instantiated with a playlist
 * - just any list of tracks.
 */
class SimpleCardWidget : NestedScrollView, MviView<CardIntentMarker, SimpleCardViewModel.ViewState> {
    val TAG = SimpleCardWidget::class.simpleName

    // viewmodel's backing model is list of tracks

    // intents
    private val eventsPublisher = PublishRelay.create<CardIntentMarker>()
    // stream to pipe in basic results (skips intent -> action processing)
    val simpleResultsPublisher = PublishRelay.create<CardResultMarker>()

    // Listener for the track rows
    private lateinit var adapter: TrackRowsAdapter
    private var onTouchListener: RecyclerTouchListener? = null
    private var onSwipeListener: RecyclerTouchListener.OnSwipeListener? = null

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_simple_card, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        expansion_header_view.setOnClickListener { v ->
            "clicked!".toast(context)
            expansionLayout.toggle(true)
        }
    }

    fun load(title: String,
             tracks: List<TrackModel>,
             swipeListener: RecyclerTouchListener.OnSwipeListener? = null,
             touchListener: RecyclerTouchListener? = null,
             carouselHeaderUrl: String?
    ) {
        onSwipeListener = swipeListener
        onTouchListener = touchListener

        // set header
        playlist_title.text = title

        carouselHeaderUrl?.let {
            // set directly onto ViewModel
            simpleResultsPublisher.accept(CardResult.HeaderSet(it))
        }

        // fetch the track stats of these pending tracks
        eventsPublisher.accept(StatsIntent.FetchStats(tracks.map { it.id }))

        // set up tracks list (don't need to re-render)
        adapter = TrackRowsAdapter(context, tracks.toMutableList())
        tracks_recycler_view.adapter = adapter
        tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        tracks_recycler_view.disableTouchTheft()
    }

    override fun intents(): Observable<out CardIntentMarker> {
        return eventsPublisher
    }

    private var carouselImageSet: Boolean = false

    override fun render(state: SimpleCardViewModel.ViewState) {
        if (state.carouselHeaderUrl != null && !carouselImageSet) {
            setCarousel(state) // set the carousel image (once)
        }

        if (state.isFetchStatsSuccess()) {
            // render the track stats widget with pending tracks
            stats_container_left.loadTrackStats(state.trackStats!!)
        }
    }

    private fun setCarousel(state: SimpleCardViewModel.ViewState) {
        if (state.carouselHeaderUrl == null || carouselImageSet) {
            return
        }

        Glide.with(this)
                .load(state.carouselHeaderUrl)
                .into(playlist_image_background)
    }
}
