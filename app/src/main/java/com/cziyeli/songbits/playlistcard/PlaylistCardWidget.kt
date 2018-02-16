package com.cziyeli.songbits.playlistcard

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v4.app.ActivityCompat.startPostponedEnterTransition
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.cziyeli.commons.Utils
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.PlaylistCardResult
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import io.reactivex.Observable
import kotlinx.android.synthetic.main.widget_expandable_tracks.view.*
import kotlinx.android.synthetic.main.widget_playlist_card.view.*
import kotlinx.android.synthetic.main.widget_quickcounts_row.view.*

/**
 * The normal playlist card (*not* the create/pending one).
 */
class PlaylistCardWidget : NestedScrollView, MviView<CardIntentMarker, PlaylistCardViewModel.PlaylistCardViewState> {
    companion object {
        val TAG = PlaylistCardWidget::class.simpleName
    }

    // models and view models
    private lateinit var playlistModel: Playlist
    private lateinit var activity: Activity

    // intents
    private var eventsPublisher = PublishRelay.create<CardIntentMarker>()

    // views
    private lateinit var adapter: TrackRowsAdapter
    private lateinit var onTouchListener: RecyclerTouchListener
    private lateinit var onSwipeListener: RecyclerTouchListener.OnSwipeListener
    private lateinit var onFabSelectedListener: OnFABMenuSelectedListener

    // flag to throttle loading
    private var startedInitialFetch: Boolean = false

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_playlist_card, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    // After the header's loaded, we'll kick off the transition and *then* start any requests.
     fun initFetching(playlist: Playlist) {
        if (startedInitialFetch) {
            return
        }

        startedInitialFetch = true

        // fetch stashed tracks -> get quick counts
        // since this is the first, we have to notify the adapter
        eventsPublisher.accept(PlaylistCardIntent.FetchSwipedTracks(
                ownerId = playlist.owner.id,
                playlistId = playlist.id,
                onlySwiped = true
        ))

        // fetch remote tracks and stats
        eventsPublisher.accept(StatsIntent.FetchTracksWithStats(playlist))
    }

    // init with the model
    fun loadPlaylist(playlist: Playlist,
                     fabSelectedListener: OnFABMenuSelectedListener,
                     tracksRecyclerViewDelegate: TracksRecyclerViewDelegate,
                     activity: Activity
    ) {
        this.activity = activity
        playlistModel = playlist
        onFabSelectedListener = fabSelectedListener
        onSwipeListener = tracksRecyclerViewDelegate.onSwipeListener
        onTouchListener = tracksRecyclerViewDelegate.onTouchListener

        // Load header
        playlist_title.text = playlistModel.name
        Glide.with(this)
                .load(playlistModel.imageUrl)
                .apply(RequestOptions
                        .noTransformation()
                        .dontAnimate()
                        .dontTransform()
                )
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        startPostponedEnterTransition(activity)
                        image_dim_overlay.visibility = View.VISIBLE
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        startPostponedEnterTransition(activity)
                        image_dim_overlay.visibility = View.VISIBLE
                        return false
                    }

                })
                .into(playlist_image_background)

        // bind the fab menu
        if (fab_menu != null && fab_button != null) {
            fab_menu!!.bindAnchorView(fab_button!!)
            fab_menu!!.setOnFABMenuSelectedListener(onFabSelectedListener)
        }

        // set up tracks list
        adapter = TrackRowsAdapter(context, mutableListOf())
        tracks_recycler_view.adapter = adapter
        tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        tracks_recycler_view.disableTouchTheft()

        // try to force refresh upon click
        // TODO this doesn't work
        expansion_header.setOnClickListener {
            Utils.mLog(TAG, "expansionHeader -- onClick! expanded? ${expansion_layout.isExpanded}")
            if (!expansion_layout.isExpanded) { // hidden, going to be opened
                adapter.notifyDataSetChanged()
            }
            expansion_layout.toggle(true)
        }
    }

    override fun intents(): Observable<out CardIntentMarker> {
        return eventsPublisher
    }

    override fun render(state: PlaylistCardViewModel.PlaylistCardViewState) {
        Utils.mLog(TAG, "RENDER", "status", "${state.status}", "last result", "${state.lastResult}")

        when {
            state.isError() -> Utils.mLog(TAG, "ERROR", "$state")
            state.isSuccess() && state.lastResult is TrackResult.ChangePrefResult -> {
                // refresh the single track row
                val track = (state.lastResult as? TrackResult.ChangePrefResult)?.currentTrack
                adapter.updateTrack(track, false)
            }
            state.isSuccess() && state.lastResult is PlaylistCardResult.FetchPlaylistTracks -> {
                if (state.stashedTracksList.isNotEmpty()) {
                    // update the title
                    expansion_header_title.text = resources.getString(R.string.expand_tracks).format(state.stashedTracksList.size)
                    Utils.setVisible(header_indicator, true)
                }
                // TODO this is very ui heavy - figure out better way than delaying until tapped
                Handler().postDelayed({
                    // only notify if this isn't expanded
                    adapter.setTracksAndNotify(state.stashedTracksList, !expansion_layout.isExpanded)
                }, 1000)
            }
            state.isSuccess() && state.lastResult is StatsResult -> {
                stats_container.loadTrackStats(state.trackStats!!)
            }
            state.isSuccess() && state.lastResult is CardResult.CalculateQuickCounts -> {
                quickstats_likes.text = "${state.likedCount} likes"
                quickstats_dislikes.text = "${state.dislikedCount} dislikes"
                quickstats_total.text = "${state.playlist.totalTracksCount} total"
                fab_text.text = "${state.unswipedCount}"

            }
        }
        checkRefreshFabMenu(state)
    }

    private fun checkRefreshFabMenu(state: PlaylistCardViewModel.PlaylistCardViewState) {
        val swipeFabItem = fab_menu.getItemById(R.id.menu_surf)
        val createFabItem = fab_menu.getItemById(R.id.menu_create_playlist)
        var shouldInvalidate = true
        when {
            state.unswipedCount == 0 && swipeFabItem?.isEnabled == true -> {
                swipeFabItem.isEnabled = false
                swipeFabItem.iconDrawable.alpha = 60
                swipeFabItem.title = resources.getString(R.string.fab_swipe_remaining, state.unswipedCount)
            }
            state.unswipedCount > 0 && swipeFabItem?.isEnabled == false -> {
                swipeFabItem.isEnabled = true
                swipeFabItem.iconDrawable.alpha = 255
                swipeFabItem.title = resources.getString(R.string.fab_swipe_remaining, state.unswipedCount)
            }
            state.tracksToCreate.isEmpty() && createFabItem?.isEnabled == true -> {
                createFabItem.isEnabled = false
                createFabItem.iconDrawable.alpha = 60
            }
            state.tracksToCreate.isNotEmpty() && createFabItem?.isEnabled == false -> {
                createFabItem.isEnabled = true
                createFabItem.iconDrawable.alpha = 255
            }
            else -> shouldInvalidate = false
        }
        if (shouldInvalidate) {
            activity.invalidateOptionsMenu()
        }
    }

    fun onBackPressed() {
        if (expansion_layout.isExpanded) Utils.setVisible(expansion_layout, false)
        if (fab_menu != null && fab_menu.isShowing) {
            fab_menu.closeMenu()
        }
    }

//    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        // to close swipe when clicking elsewhere
//        if (touchListener != null) touchListener!!.getTouchCoordinates(ev)
//        return super.dispatchTouchEvent(ev)
//    }

}