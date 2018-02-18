package com.cziyeli.songbits.playlistcard.create

import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.synnapps.carouselview.ViewListener
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_playlistcard_create.*
import kotlinx.android.synthetic.main.widget_expandable_tracks.*
import kotlinx.android.synthetic.main.widget_playlist_card_create.*
import org.jetbrains.anko.intentFor
import java.util.*
import javax.inject.Inject
import javax.inject.Named



/**
 * Activity holding the [PlaylistCardCreateWidget] for creating a playlist out of tracks.
 */
class PlaylistCardCreateActivity : AppCompatActivity(),
        PlaylistCardCreateWidget.PlaylistCreatedListener,
        TracksRecyclerViewDelegate.ActionButtonListener {

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
    private var carouselHeaderUrl: String? = null
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var tracksRecyclerViewDelegate: TracksRecyclerViewDelegate

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

        // grab random track's header as the carousel header
        carouselHeaderUrl = when {
            viewModel.currentViewState.carouselHeaderUrl != null -> viewModel.currentViewState.carouselHeaderUrl
            savedInstanceState?.getString(EXTRA_HEADER_URL) != null -> savedInstanceState.getString(EXTRA_HEADER_URL)
            else -> {
                val headerImageIndex = Random().nextInt(initialPendingTracks.size)
                initialPendingTracks[headerImageIndex].imageUrl
            }
        }

        // set up the widget with the viewmodel's tracks
        create_playlist_card_widget.loadTracks(
                viewModel.pendingTracks,
               tracksRecyclerViewDelegate,
                carouselHeaderUrl,
                this
        )
        // delegate ui events to the mvi view
        fab.setOnClickListener { _ ->
            create_playlist_card_widget.createPlaylist(App.getCurrentUserId(), viewModel.pendingTracks)
        }
    }

    override fun onPlaylistCreated() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun initViewModel(viewModel: PlaylistCardCreateViewModel) {
        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())

        // Bind ViewModel to simple results stream
        viewModel.processSimpleResults(create_playlist_card_widget.simpleResultsPublisher)
    }

    private fun render(state: PlaylistCardCreateViewModel.ViewState) {
        create_playlist_card_widget.render(state)
    }

    private fun intents(): Observable<out CardIntentMarker> {
        return create_playlist_card_widget.intents()
    }

    override fun onResume() {
        super.onResume()
        tracks_recycler_view.addOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_HEADER_URL, carouselHeaderUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        tracks_recycler_view.removeOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

}