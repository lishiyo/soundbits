package com.cziyeli.songbits.playlistcard.create

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.playlistcard.SinglePlaylistIntent
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import com.synnapps.carouselview.ViewListener
import dagger.android.AndroidInjection
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_playlistcard_create.*
import kotlinx.android.synthetic.main.widget_playlist_card_create.*
import org.jetbrains.anko.intentFor
import javax.inject.Inject
import javax.inject.Named


/**
 * Activity holding the [PlaylistCardCreateWidget] for creating a playlist out of tracks.
 */
class PlaylistCardCreateActivity : AppCompatActivity() {
    val TAG = PlaylistCardCreateActivity::class.java.simpleName

    companion object {
        const val NUMBER_OF_PAGES = 2
        const val EXTRA_PENDING_TRACKS = "extra_pending_tracks"

        fun create(context: Context, pendingTracks: List<TrackModel>? = listOf()) : Intent {
            return context.intentFor<PlaylistCardCreateActivity>(PlaylistCardCreateActivity.EXTRA_PENDING_TRACKS to pendingTracks)
        }
    }

    // pending tracks to create/add
    lateinit var pendingTracks: List<TrackModel>

    @Inject
    @field:Named("PlaylistCardCreateViewModel") lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PlaylistCardCreateViewModel

    // viewpager
    private val viewListener: ViewListener = ViewListener {
        if (it == 0) {
            layoutInflater.inflate(R.layout.playlist_header_create_new, null)
        } else {
            layoutInflater.inflate(R.layout.playlist_header_add_existing, null)
        }
    }
    private lateinit var onTouchListener: RecyclerTouchListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playlistcard_create)

        pendingTracks = intent.getParcelableArrayListExtra(EXTRA_PENDING_TRACKS)

        // inject AFTER parsing out the tracks so the viewmodel starts out with it
        AndroidInjection.inject(this)

        // set up the header
        create_header_carousel.pageCount = NUMBER_OF_PAGES
        create_header_carousel.setViewListener(viewListener)
        create_header_carousel.setImageClickListener {
            Toast.makeText(this@PlaylistCardCreateActivity, "Clicked: $it", Toast.LENGTH_SHORT).show()
        }

        // bind the viewmodel, passing through to the subviews
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PlaylistCardCreateViewModel::class.java)
        initViewModel(viewModel)

        // set up the widget
        onTouchListener = createOnTouchListener()
        create_playlist_card_widget.loadTracks(
                pendingTracks,
                null,
                onTouchListener
        )
    }

    private fun createOnTouchListener() : RecyclerTouchListener {
        val onTouchListener = RecyclerTouchListener(this, create_tracks_recycler_view)
        onTouchListener
                .setViewsToFade(R.id.track_status)
                .setSwipeable(false) // Create is read-only!

        return onTouchListener
    }

    private fun initViewModel(viewModel: PlaylistCardCreateViewModel) {
        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(this, Observer { state ->
            state?.let {
                // pass to the widget
                render(state)
            }
        })

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
    }

    private fun render(state: PlaylistCardCreateViewModel.ViewState) {
        create_playlist_card_widget.render(state)
    }

    private fun intents(): Observable<out SinglePlaylistIntent> {
        return create_playlist_card_widget.intents()
    }

//    private fun createOnSwipeListener() : RecyclerTouchListener.OnSwipeListener {
//        return object : RecyclerTouchListener.OnSwipeListener {
//            override fun onForegroundAnimationStart(isFgOpening: Boolean, duration: Long, foregroundView: View, backgroundView: View?) {
//                // shrink the textview size
//                val scale = if (isFgOpening) 0.7f else 1.0f
//                val parentView = foregroundView as ViewGroup
//                val shrinkingViews = listOf<View>(
//                        parentView.findViewById(R.id.track_left_container),
//                        parentView.findViewById(R.id.track_image)
//                )
//                shrinkingViews.forEach { view ->
//                    view.pivotX = 0f
//                    view.animate()
//                            .scaleX(scale)
//                            .scaleY(scale)
//                            .setDuration(duration)
//                            .start()
//                }
//
//                val animatedView = foregroundView.findViewById<LottieAnimationView>(R.id.wave_animation)
//                val toAlpha = if (isFgOpening) 1.0f else 0.0f
//                animatedView.animate().alpha(toAlpha).withEndAction {
//                    if (isFgOpening) {
//                        animatedView.visibility = View.VISIBLE
//                        animatedView.playAnimation()
//                    } else {
//                        animatedView.visibility = View.INVISIBLE
//                        animatedView.pauseAnimation()
//                    }
//                }.setDuration(duration).start()
//            }
//
//            override fun onSwipeOptionsOpened(foregroundView: View?, backgroundView: View?) {
//                Log.i(DTAG, "onSwipeOptionsOpened")
//            }
//
//            override fun onSwipeOptionsClosed(foregroundView: View?, backgroundView: View?) {
//                Log.i(DTAG, "onSwipeOptionsClosed")
//            }
//        }
//    }
//
//    private fun createOnTouchListener(swipeListener: RecyclerTouchListener.OnSwipeListener) : RecyclerTouchListener {
//        val onTouchListener = RecyclerTouchListener(this, create_tracks_recycler_view)
//        onTouchListener
//                .setViewsToFade(R.id.track_status)
//                .setOnSwipeListener(swipeListener)
//                .setSwipeable(false) // Create is read-only!
//
//        return onTouchListener
//    }

    override fun onResume() {
        super.onResume()
        create_tracks_recycler_view.addOnItemTouchListener(onTouchListener)
    }

    override fun onPause() {
        super.onPause()
        create_tracks_recycler_view.removeOnItemTouchListener(onTouchListener)
    }
}