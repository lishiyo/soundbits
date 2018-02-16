package com.cziyeli.songbits.playlistcard.create

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.cards.summary.SummaryIntent
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.jakewharton.rxrelay2.PublishRelay
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import io.reactivex.Observable
import io.saeid.fabloading.LoadingView
import kotlinx.android.synthetic.main.playlist_header_add_existing.view.*
import kotlinx.android.synthetic.main.playlist_header_create_new.view.*
import kotlinx.android.synthetic.main.widget_playlist_card_create.view.*

/**
 * View for creating a playlist/adding to existing playlist out of a list of tracks.
 * Read-only tracks, no fab menu.
 */
class PlaylistCardCreateWidget : NestedScrollView, MviView<CardIntentMarker, PlaylistCardCreateViewModel.ViewState> {
    private val TAG = PlaylistCardCreateViewModel::class.java.simpleName

    companion object {
        val FAB_CREATE_COLOR_0 = App.appComponent.appContext().resources.getColor(R.color.colorFab)
        val FAB_CREATE_COLOR_1 = App.appComponent.appContext().resources.getColor(R.color.colorPurple)
        val FAB_CREATE_COLOR_2 = App.appComponent.appContext().resources.getColor(R.color.colorAccent)
        val FAB_CREATE_COLOR_3 = App.appComponent.appContext().resources.getColor(R.color.venice_verde)
    }

    // intents
    private val eventsPublisher = PublishRelay.create<CardIntentMarker>()
    // stream to pipe in basic results (skips intent -> action processing)
    val simpleResultsPublisher: PublishRelay<CardResultMarker> = PublishRelay.create<CardResultMarker>()

    private lateinit var adapter: TrackRowsAdapter
    private var onTouchListener: RecyclerTouchListener? = null
    private var onSwipeListener: RecyclerTouchListener.OnSwipeListener? = null
    private var playlistCreatedListener: PlaylistCreatedListener? = null

    // Flag for whether we've created the new playlist or not
    private var isFinished: Boolean = false
    private var carouselImageSet: Boolean = false

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_playlist_card_create, this, true)
    }

    fun loadTracks(tracks: List<TrackModel>,
                   tracksRecyclerViewDelegate: TracksRecyclerViewDelegate,
                   carouselHeaderUrl: String?,
                   listener: PlaylistCreatedListener? = null
    ) {
        onSwipeListener = tracksRecyclerViewDelegate.onSwipeListener
        onTouchListener = tracksRecyclerViewDelegate.onTouchListener
        playlistCreatedListener = listener

        carouselHeaderUrl?.let {
            simpleResultsPublisher.accept(CardResult.HeaderSet(it))
        }
        create_card_label_2.text = resources.getString(R.string.create_card_label_2).format(tracks.size)

        // set up the loading fab
        fab.addAnimation(FAB_CREATE_COLOR_0, R.drawable.basic_plus_fab,
                LoadingView.FROM_LEFT)
        fab.addAnimation(FAB_CREATE_COLOR_1, R.drawable.basic_play_fab,
                LoadingView.FROM_LEFT)
        fab.addAnimation(FAB_CREATE_COLOR_2, R.drawable.basic_pause_fab,
                LoadingView.FROM_TOP)
        fab.addAnimation(FAB_CREATE_COLOR_3, R.drawable.basic_stop_1_fab,
                LoadingView.FROM_RIGHT)

        // fetch the track stats of these pending tracks
        eventsPublisher.accept(StatsIntent.FetchStats(tracks.map { it.id }))

        // set up tracks list (don't need to re-render)
        adapter = TrackRowsAdapter(context, tracks.toMutableList())
        create_tracks_recycler_view.adapter = adapter
        create_tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        create_tracks_recycler_view.disableTouchTheft()
    }

    override fun render(state: PlaylistCardCreateViewModel.ViewState) {
        if (state.carouselHeaderUrl != null && !carouselImageSet) {
            setCarousel(state) // set the carousel image (once)
        }

        when {
            state.isFetchStatsSuccess() -> {
                // render the track stats widget with pending tracks
                create_stats_container.loadTrackStats(state.trackStats!!)
            }
            state.status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS -> {
                "success!".toast(context)
                onPlaylistCreated(create_playlist_new_title.text.toString(), state)
                fab.pauseAnimation()
                fab.setImageResource(R.drawable.note_happy_colored) // switch fab
            }
            state.isCreateFinished() -> {
                fab.pauseAnimation()
            }
            state.isCreateLoading() -> {
                fab.resumeAnimation()
            }
            state.isError() -> "something went wrong".toast(context)
        }
    }

    fun createPlaylist(ownerId: String, tracks: List<TrackModel>) {
        if (isFinished) {
            return
        }

        if (create_playlist_new_title.text.isBlank()) {
            "you have to give your playlist a name!".toast(context)
            return
        }

        fab.startAnimation()
        eventsPublisher.accept(SummaryIntent.CreatePlaylistWithTracks(
                ownerId = ownerId,
                name = create_playlist_new_title.text.toString(),
                description = resources.getString(R.string.new_playlist_description),
                public = false,
                tracks = tracks
        ))
    }

    private fun onPlaylistCreated(newTitle: String, state: PlaylistCardCreateViewModel.ViewState) {
        isFinished = true
        create_card_view.cardElevation = resources.getDimension(R.dimen.playlist_card_finished_elevation)
        Utils.hideKeyboard(context, create_playlist_new_title)

        // set title and image
        Utils.setVisible(playlist_header_finished, true) // set up in background
        val titleView = playlist_header_finished.findViewById<TextView>(R.id.finished_playlist_new_title)
        val imageView = playlist_header_finished.findViewById<ImageView>(R.id.finished_playlist_image_background)
        titleView.text = newTitle
        Glide.with(this).load(state.carouselHeaderUrl).into(imageView)

        Utils.setVisible(create_header_carousel, false)
        create_image_dim_overlay.alpha = 0.4f

        isEnabled = false
        descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        // notify calling activity
        playlistCreatedListener?.run {
            onPlaylistCreated()
        }
    }

    override fun intents(): Observable<out CardIntentMarker> {
        return eventsPublisher
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        return isFinished
    }

    private fun setCarousel(state: PlaylistCardCreateViewModel.ViewState) {
        if (state.carouselHeaderUrl == null || carouselImageSet) {
            return
        }

        if (create_header_carousel.findViewById<View>(R.id.create_playlist_image_background) != null) {
            Glide.with(this).load(state.carouselHeaderUrl).into(create_playlist_image_background)
            carouselImageSet = true
        }
        if (create_header_carousel.findViewById<View>(R.id.add_playlist_image_background) != null) {
            Glide.with(this).load(state.carouselHeaderUrl).into(add_playlist_image_background)
            carouselImageSet = true
        }
    }

    interface PlaylistCreatedListener {
        fun onPlaylistCreated()
    }
}