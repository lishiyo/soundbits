package com.cziyeli.songbits.home.detail

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
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
import com.cziyeli.commons.disableTouchTheft
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import kotlinx.android.synthetic.main.widget_playlist_card.view.*

/**
 * The normal playlist card (*not* the create/pending one).
 */
class PlaylistCardWidget : NestedScrollView {

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    private lateinit var playlistModel: Playlist
    private lateinit var adapter: TrackRowsAdapter
    private lateinit var onTouchListener: RecyclerTouchListener
    private lateinit var onFabSelectedListener: OnFABMenuSelectedListener
    private lateinit var onSwipeListener: RecyclerTouchListener.OnSwipeListener

    companion object {
        private const val FAB_ASSET_FILE = "emoji_wink.json"
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_playlist_card, this, true)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    fun loadPlaylist(playlist: Playlist,
                     fabSelectedListener: OnFABMenuSelectedListener,
                     swipeListener: RecyclerTouchListener.OnSwipeListener,
                     touchListener: RecyclerTouchListener,
                     activity: Activity) {
        playlistModel = playlist
        onFabSelectedListener = fabSelectedListener
        onSwipeListener = swipeListener
        onTouchListener = touchListener

        // Load header
        playlist_title.text = playlistModel.name
        Glide.with(this)
                .load(playlistModel.simpleImage?.url)
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

        // load the fab
//        setupLottieFab()
        if (fab_menu != null && fab_button != null) {
            fab_menu!!.bindAnchorView(fab_button!!)
            fab_menu!!.setOnFABMenuSelectedListener(onFabSelectedListener)
        }

        // add the dummy track stats
        stats_container.loadDefaultAudioFeatures()

        // set up tracks list
        adapter = TrackRowsAdapter(context, getDummyTracks())
        tracks_recycler_view.adapter = adapter
        tracks_recycler_view.layoutManager = LinearLayoutManager(context)
        tracks_recycler_view.disableTouchTheft()
    }

//    private fun setupLottieFab() {
//        // set up lottie animated fake fab view
////        fab.imageAssetsFolder = "headphones_images/";
//        fab.setAnimation(FAB_ASSET_FILE)
////        val colorFilter = SimpleColorFilter(colorFromRes(R.color.colorWhite))
////        (fab as LottieAnimationView).colorFilter = colorFilter
//        fab.repeatCount = LottieDrawable.INFINITE
//        fab.playAnimation()
//    }

    fun onBackPressed() {
        if (fab_menu != null) {
            if (fab_menu.isShowing) {
                fab_menu.closeMenu()
            }
        }
    }

//    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        // to close swipe when clicking elsewhere
//        if (touchListener != null) touchListener!!.getTouchCoordinates(ev)
//        return super.dispatchTouchEvent(ev)
//    }
}