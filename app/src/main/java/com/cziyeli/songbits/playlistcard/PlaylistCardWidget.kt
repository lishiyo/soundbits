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
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlistcard.PlaylistCardResult
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.widget_playlist_card.view.*

/**
 * The normal playlist card (*not* the create/pending one).
 */
class PlaylistCardWidget : NestedScrollView, MviView<SinglePlaylistIntent, PlaylistCardViewModel.PlaylistCardViewState> {
    companion object {
        val TAG = PlaylistCardWidget::class.simpleName
        private const val FAB_ASSET_FILE = "emoji_wink.json"
    }

    // models and view models
    private lateinit var playlistModel: Playlist
    private lateinit var activity: Activity

    // intents
    private val eventsPublisher = PublishSubject.create<SinglePlaylistIntent>()

    // views
    private lateinit var adapter: TrackRowsAdapter
    private lateinit var onTouchListener: RecyclerTouchListener
    private lateinit var onFabSelectedListener: OnFABMenuSelectedListener
    private lateinit var onSwipeListener: RecyclerTouchListener.OnSwipeListener

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
        eventsPublisher.onNext(PlaylistCardIntent.FetchSwipedTracks(
                ownerId = playlist.owner.id,
                playlistId = playlist.id,
                onlySwiped = true)
        )

        // fetch remote tracks and stats
        eventsPublisher.onNext(StatsIntent.FetchTracksWithStats(playlist))
    }

    // init with the model
    fun loadPlaylist(playlist: Playlist,
                     fabSelectedListener: OnFABMenuSelectedListener,
                     swipeListener: RecyclerTouchListener.OnSwipeListener,
                     touchListener: RecyclerTouchListener,
                     activity: Activity) {
        this.activity = activity
        playlistModel = playlist
        onFabSelectedListener = fabSelectedListener
        onSwipeListener = swipeListener
        onTouchListener = touchListener

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

        // bing the fab menu
        if (fab_menu != null && fab_button != null) {
            fab_menu!!.bindAnchorView(fab_button!!)
            fab_menu!!.setOnFABMenuSelectedListener(onFabSelectedListener)
        }

        // set up tracks list
        adapter = TrackRowsAdapter(context, mutableListOf())
        tracks_recycler_view.adapter = adapter
        tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        tracks_recycler_view.disableTouchTheft()
    }

    override fun intents(): Observable<out SinglePlaylistIntent> {
        return eventsPublisher
    }

    override fun render(state: PlaylistCardViewModel.PlaylistCardViewState) {
        if (state.isError()) {
            "error rendering: ${state.error}".toast(context)
        }

        // if we just loaded tracks
        if (state.status == PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS) {
//            Utils.mLog(TAG, "RENDER", "just got playlist tracks! stashed: ${state.stashedTracksList.size} all: ${state.allTracksList.size}")
            if (state.stashedTracksList.isNotEmpty()) {
                // calculating the likes/dislikes of the stashed tracks
                eventsPublisher.onNext(PlaylistCardIntent.CalculateQuickCounts(playlistModel, state.stashedTracksList))
            }

            // render the track rows
            // TODO this is very ui heavy - figure out better way than delaying until tapped
            Handler().postDelayed({
                adapter.setTracksAndNotify(state.stashedTracksList.map { TrackRow(it) })
            }, 1000)
        }

        // render the quick counts
        quickstats_likes.text = "${state.likedCount} likes"
        quickstats_dislikes.text = "${state.dislikedCount} dislikes"
        quickstats_total.text = "${state.playlist.totalTracksCount} total"
        fab_text.text = "${state.unswipedCount}"

        // check if we need to disable/hide any fab menu items
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

        // render the track stats widget with remote tracks
        if (state.isSuccess() && state.trackStats != null) {
            Utils.mLog(TAG, "RENDER", "just got track stats! loading....")
            stats_container.loadTrackStats(state.trackStats!!)
        }
    }

    fun onBackPressed() {
        if (expansionLayout.isExpanded) Utils.setVisible(expansionLayout, false)
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