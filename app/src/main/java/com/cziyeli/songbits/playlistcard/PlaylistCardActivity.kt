package com.cziyeli.songbits.playlistcard

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.cziyeli.commons.toast
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.CardsIntent
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_playlistcard.*
import kotlinx.android.synthetic.main.widget_expandable_tracks.*
import kotlinx.android.synthetic.main.widget_playlist_card.*
import org.jetbrains.anko.intentFor
import javax.inject.Inject
import javax.inject.Named

class PlaylistCardActivity : AppCompatActivity(), TracksRecyclerViewDelegate.ActionButtonListener {


    val TAG = PlaylistCardActivity::class.java.simpleName
    companion object {
        const val EXTRA_PLAYLIST_ITEM = "extra_playlist_item"

        fun create(context: Context, playlist: Playlist) : Intent {
            return context.intentFor<PlaylistCardActivity>(PlaylistCardActivity.EXTRA_PLAYLIST_ITEM to playlist)
        }
    }

    // the model backing this card
    lateinit var playlist: Playlist
    // Listener for the FAB menu
    private val onFabMenuSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_surf -> {
                startSwipingTracks(false)
            }
            R.id.menu_resurf -> {
                startSwipingTracks(true)
            }
            R.id.menu_create_playlist -> {
                if (viewModel.tracksToCreate?.isEmpty() == true) {
                    "no liked tracks yet! swipe first?".toast(this@PlaylistCardActivity)
                } else {
                    // go to create with liked tracks
                    startActivity(PlaylistCardCreateActivity.create(
                            this@PlaylistCardActivity, viewModel.tracksToCreate)
                    )
                }
            }
        }
    }

    @Inject
    @field:Named("PlaylistCardViewModel") lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PlaylistCardViewModel

    private val eventsPublisher: PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var tracksRecyclerViewDelegate: TracksRecyclerViewDelegate

    @Inject @field:Named("Native") lateinit var mPlayer: PlayerInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Postpone the enter transition until image is loaded
        postponeEnterTransition()

        setContentView(R.layout.activity_playlistcard)

        // load the parceled item info (image, text etc)
        playlist = intent.getParcelableExtra(EXTRA_PLAYLIST_ITEM)

        // inject AFTER parsing out the model
        AndroidInjection.inject(this)

        // bind the viewmodel, passing through to the subviews
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PlaylistCardViewModel::class.java)
        initViewModel(viewModel)
        playlist_card_widget.loadPlaylist(playlist, onFabMenuSelectedListener,
                tracksRecyclerViewDelegate, this)

        playlist_card_widget.setOnTouchListener { v, event ->
            if (fab_menu.isShowing) {
                fab_menu.closeMenu()
            }
            false
        }
    }

    /**
     * Start the tinder UI.
     */
    private fun startSwipingTracks(reswipeAll: Boolean = false) {
        viewModel.playlist?.run {
            if (reswipeAll) {
                startActivity(CardsActivity.create(this@PlaylistCardActivity, this))
            } else {
                startActivity(CardsActivity.create(
                        this@PlaylistCardActivity, this, viewModel.tracksToSwipe!!))
            }
        }
    }

    private fun initViewModel(viewModel: PlaylistCardViewModel) {
        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state) // pass to the widgets
                    }
                })
        )

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
    }

    private fun render(state: PlaylistCardViewModel.PlaylistCardViewState) {
        playlist_card_widget.render(state)
    }

    private fun intents(): Observable<out CardIntentMarker> {
        return Observable.merge(playlist_card_widget.intents(), eventsPublisher)
    }

    override fun onLiked(position: Int) {
        val model = viewModel.currentViewState.stashedTracksList?.get(position)
        // only update if not already liked +
        if (!model.liked) {
            val newModel = model.copy(pref = TrackModel.Pref.LIKED)
            eventsPublisher.accept(CardsIntent.ChangeTrackPref.like(newModel))
        }
    }

    override fun onDisliked(position: Int) {
        val model = viewModel.currentViewState.stashedTracksList?.get(position)
        // only update if not already disliked
        if (model.liked) {
            val newModel = model.copy(pref = TrackModel.Pref.DISLIKED)
            eventsPublisher.accept(CardsIntent.ChangeTrackPref.dislike(newModel))
        }
    }

    override fun onSwipeClosed(model: TrackModel) {
        super.onSwipeClosed(model)
        eventsPublisher.accept(
                CardsIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))
    }

    override fun onSwipeOpen(model: TrackModel) {
        super.onSwipeOpen(model)
        eventsPublisher.accept(
                CardsIntent.CommandPlayer.create(PlayerInterface.Command.PLAY_NEW, model))
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()

        playlist_card_widget.initFetching(playlist)
    }

    override fun onBackPressed() {
        playlist_card_widget.onBackPressed()
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        tracks_recycler_view.addOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
        mPlayer.apply { onResume() }
    }

    override fun onPause() {
        super.onPause()
        tracks_recycler_view.removeOnItemTouchListener(tracksRecyclerViewDelegate.onTouchListener)
        mPlayer.apply { onPause() }
    }

    override fun onDestroy() {
        mPlayer.apply { onDestroy() }
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        val b = outState ?: Bundle()
        b.putParcelable(EXTRA_PLAYLIST_ITEM, playlist)
        super.onSaveInstanceState(b)
    }

}