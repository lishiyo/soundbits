package com.cziyeli.songbits.playlistcard.create

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.SinglePlaylistIntent
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import com.synnapps.carouselview.ViewListener
import dagger.android.AndroidInjection
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_playlistcard_create.*
import kotlinx.android.synthetic.main.widget_playlist_card_create.*
import org.jetbrains.anko.intentFor
import java.util.*
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
        const val EXTRA_HEADER_URL = "extra_header_url"

        fun create(context: Context, pendingTracks: List<TrackModel>? = listOf()) : Intent {
            return context.intentFor<PlaylistCardCreateActivity>(PlaylistCardCreateActivity.EXTRA_PENDING_TRACKS to pendingTracks)
        }
    }

    // pending tracks to create/add
    lateinit var initialPendingTracks: List<TrackModel>

    @Inject
    @field:Named("PlaylistCardCreateViewModel") lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PlaylistCardCreateViewModel

    // viewpager
    private val viewListener: ViewListener = ViewListener {
        val view: View
        if (it == 0) {
            view = layoutInflater.inflate(R.layout.playlist_header_create_new, create_header_carousel, false)
        } else {
            view = layoutInflater.inflate(R.layout.playlist_header_add_existing, create_header_carousel, false)

        }
        view
    }
    private lateinit var onTouchListener: RecyclerTouchListener

    private var carouselHeaderUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playlistcard_create)

        initialPendingTracks = intent.getParcelableArrayListExtra<TrackModel>(EXTRA_PENDING_TRACKS) as List<TrackModel>

        // inject AFTER parsing out the tracks so the viewmodel starts out with it
        AndroidInjection.inject(this)

        // set up the header
        create_header_carousel.pageCount = NUMBER_OF_PAGES
        create_header_carousel.setViewListener(viewListener)

        // bind the viewmodel, passing through to the subviews
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PlaylistCardCreateViewModel::class.java)
        initViewModel(viewModel)

        // store random track image
        carouselHeaderUrl = if (savedInstanceState?.getString(EXTRA_HEADER_URL) != null) {
            savedInstanceState.getString(EXTRA_HEADER_URL)
        } else if (viewModel.states().value?.carouselHeaderUrl != null) {
            viewModel.states().value?.carouselHeaderUrl
        } else {
            val headerImageIndex = Random().nextInt(initialPendingTracks.size)
            initialPendingTracks[headerImageIndex].imageUrl
        }

        // set up the widget with the viewmodel's tracks
        onTouchListener = createOnTouchListener()
        create_playlist_card_widget.loadTracks(
                viewModel.pendingTracks,
                null,
                onTouchListener,
                carouselHeaderUrl
        )
        // delegate ui events to the mvi view
        fab.setOnClickListener { _ ->
            create_playlist_card_widget.createPlaylist(App.getCurrentUserId(), viewModel.pendingTracks)
        }
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

        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())
    }

    private fun render(state: PlaylistCardCreateViewModel.ViewState) {
        create_playlist_card_widget.render(state)
    }

    private fun intents(): Observable<out SinglePlaylistIntent> {
        return create_playlist_card_widget.intents()
    }

    override fun onResume() {
        super.onResume()
        create_tracks_recycler_view.addOnItemTouchListener(onTouchListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_HEADER_URL, carouselHeaderUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        create_tracks_recycler_view.removeOnItemTouchListener(onTouchListener)
    }
}