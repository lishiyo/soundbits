package com.cziyeli.songbits.playlistcard.create

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.toast
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.summary.SummaryIntent
import com.cziyeli.songbits.playlistcard.SinglePlaylistIntent
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.cziyeli.songbits.playlistcard.TrackRow
import com.cziyeli.songbits.playlistcard.TrackRowsAdapter
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.playlist_header_create_new.view.*
import kotlinx.android.synthetic.main.widget_playlist_card_create.view.*

/**
 * View for creating a playlist/adding to existing playlist out of a list of tracks.
 * Read-only tracks, no fab menu.
 */
class PlaylistCardCreateWidget : NestedScrollView, MviView<SinglePlaylistIntent, PlaylistCardCreateViewModel.ViewState> {
    private val TAG = PlaylistCardCreateViewModel::class.java.simpleName

    // intents
    private val eventsPublisher = PublishSubject.create<SinglePlaylistIntent>()

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    private lateinit var adapter: TrackRowsAdapter
    private var onTouchListener: RecyclerTouchListener? = null
    private var onSwipeListener: RecyclerTouchListener.OnSwipeListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_playlist_card_create, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    fun loadTracks(tracks: List<TrackModel>,
                   swipeListener: RecyclerTouchListener.OnSwipeListener? = null,
                   touchListener: RecyclerTouchListener? = null) {
        onSwipeListener = swipeListener
        onTouchListener = touchListener

        // fetch the track stats of these pending tracks
        eventsPublisher.onNext(StatsIntent.FetchStats(tracks.map { it.id }))

        // set up tracks list (don't need to re-render)
        adapter = TrackRowsAdapter(context, tracks.map { TrackRow(it) }.toMutableList())
        create_tracks_recycler_view.adapter = adapter
        create_tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        create_tracks_recycler_view.disableTouchTheft()
    }

    fun createPlaylist(ownerId: String, tracks: List<TrackModel>) {
        if (create_playlist_new_title.text.isBlank()) {
            "you have to give your playlist a name!".toast(context)
            return
        }

        eventsPublisher.onNext(SummaryIntent.CreatePlaylistWithTracks(
                    ownerId = ownerId,
                    name = create_playlist_new_title.text.toString(),
                    description = "via songbits",
                    public = false,
                    tracks = tracks)
            )
    }

    override fun intents(): Observable<out SinglePlaylistIntent> {
        return eventsPublisher
    }

    override fun render(state: PlaylistCardCreateViewModel.ViewState) {
        fab_text.text = "${state.pendingTracks.size}"

        // render the track stats widget with remote tracks
        if (state.isSuccess() && state.trackStats != null) {
            create_stats_container.loadTrackStats(state.trackStats!!)
        }
    }

}