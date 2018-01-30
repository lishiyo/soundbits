package com.cziyeli.songbits.home.detail

import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import eu.gsottbauer.equalizerview.EqualizerView
import kotlinx.android.synthetic.main.activity_playlistcard.*
import kotlinx.android.synthetic.main.widget_playlist_card.*

class PlaylistCardActivity : AppCompatActivity() {
    val TAG = PlaylistCardActivity.javaClass.simpleName.toString()
    companion object {
        const val EXTRA_PLAYLIST_ITEM = "playlist_item"
    }

    // the model backing this card
    private lateinit var playlistModel: Playlist
    // Listener for the track rows
    private lateinit var onSwipeListener: RecyclerTouchListener.OnSwipeListener
    private lateinit var onTouchListener: RecyclerTouchListener
    // Listener for the FAB menu
    private val onFabMenuSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_surf -> Toast.makeText(this@PlaylistCardActivity, "Surf Selected", Toast.LENGTH_SHORT).show()
            R.id.menu_resurf -> Toast.makeText(this@PlaylistCardActivity, "Resurf Selected", Toast.LENGTH_SHORT).show()
            R.id.menu_create_playlist -> Toast.makeText(this@PlaylistCardActivity, "Create Selected", Toast.LENGTH_SHORT).show()
        }
        val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                fab_button,
                ViewCompat.getTransitionName(fab_button)
        ).toBundle()

        // go to create
        // todo send list of parceled tracks
//        val intent = Intent(this@PlaylistCardActivity, PlaylistCardCreateActivity::class.java)
//        startActivity(intent, bundle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Postpone the enter transition until image is loaded
        postponeEnterTransition()

        setContentView(R.layout.activity_playlistcard)

        // load the parceled item info (image, text etc)
        playlistModel = intent.getParcelableExtra(EXTRA_PLAYLIST_ITEM)
        onSwipeListener = createOnSwipeListener()
        onTouchListener = createOnTouchListener(onSwipeListener)

        // set up the widget
        playlist_card_widget.loadPlaylist(playlistModel, onFabMenuSelectedListener, onSwipeListener, onTouchListener,this)
    }

    private fun createOnSwipeListener() : RecyclerTouchListener.OnSwipeListener {
        return object : RecyclerTouchListener.OnSwipeListener {
            override fun onForegroundAnimationStart(isFgOpening: Boolean, duration: Long, foregroundView: View, backgroundView: View?) {
                // shrink the textview size
                val scale = if (isFgOpening) 0.7f else 1.0f
                val parentView = foregroundView as ViewGroup
                val shrinkingViews = listOf<View>(
                        parentView.findViewById(R.id.track_left_container),
                        parentView.findViewById(R.id.track_image)
                )
                shrinkingViews.forEach { view ->
                    view.pivotX = 0f
                    view.animate()
                            .scaleX(scale)
                            .scaleY(scale)
                            .setDuration(duration)
                            .start()
                }

                // animate the wave
                val toAlpha = if (isFgOpening) 1.0f else 0.0f
                val animatedView = foregroundView.findViewById<EqualizerView>(R.id.equalizer_animation)

//                val animatedView = foregroundView.findViewById<LottieAnimationView>(R.id.wave_animation)
                animatedView.animate().alpha(toAlpha).withEndAction {
                    if (isFgOpening) {
                        animatedView.visibility = View.VISIBLE
                        animatedView.animateBars();
//                        animatedView.playAnimation()
                    } else {
                        animatedView.visibility = View.INVISIBLE
                        animatedView.stopBars()
//                        animatedView.pauseAnimation()
                    }
                }.setDuration(duration).start()
            }

            override fun onSwipeOptionsOpened(foregroundView: View, backgroundView: View?) {

            }

            override fun onSwipeOptionsClosed(foregroundView: View, backgroundView: View?) {

            }

        }
    }

    private fun createOnTouchListener(swipeListener: RecyclerTouchListener.OnSwipeListener) : RecyclerTouchListener {
        val onTouchListener = RecyclerTouchListener(this, tracks_recycler_view)
        onTouchListener
                .setViewsToFade(R.id.track_status)
//                .setClickable(object : RecyclerTouchListener.OnRowClickListener {
//                    override fun onRowClicked(position: Int) {
//                        Log.i(DTAG, "row @ ${position} clicked")
//                    }
//
//                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {
//                        Log.i(DTAG, "independent view @ ${position} clicked")
//                    }
//                })
//                .setLongClickable(true) { position ->
//                    Log.i(DTAG, "row @ ${position} long clicked")
//                }
                .setOnSwipeListener(swipeListener)
                .setSwipeOptionViews(R.id.like_icon_container, R.id.dislike_icon_container)
                .setSwipeable(R.id.row_foreground, R.id.row_background) { viewID, position ->
                    var message = ""
                    when (viewID) {
                        R.id.like_icon_container -> message += "Like"
                        R.id.dislike_icon_container -> message += "Dislike"
                    }
                    message += " clicked for row " + (position + 1)
                    Log.i(TAG, message)
                }

        return onTouchListener
    }


    override fun onBackPressed() {
        playlist_card_widget.onBackPressed()
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        tracks_recycler_view.addOnItemTouchListener(onTouchListener)
    }

    override fun onPause() {
        super.onPause()
        tracks_recycler_view.removeOnItemTouchListener(onTouchListener)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        val b = outState ?: Bundle()
        b.putParcelable(EXTRA_PLAYLIST_ITEM, playlistModel)
        super.onSaveInstanceState(b)
    }

}