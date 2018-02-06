package com.cziyeli.songbits.playlistcard

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.cards.TrackIntent
import com.cziyeli.songbits.playlistcard.create.PlaylistCardCreateActivity
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import dagger.android.AndroidInjection
import eu.gsottbauer.equalizerview.EqualizerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_playlistcard.*
import kotlinx.android.synthetic.main.widget_playlist_card.*
import org.jetbrains.anko.intentFor
import javax.inject.Inject
import javax.inject.Named

class PlaylistCardActivity : AppCompatActivity() {
    val TAG = PlaylistCardActivity::class.java.simpleName
    companion object {
        const val EXTRA_PLAYLIST_ITEM = "extra_playlist_item"

        fun create(context: Context, playlist: Playlist) : Intent {
            return context.intentFor<PlaylistCardActivity>(PlaylistCardActivity.EXTRA_PLAYLIST_ITEM to playlist)
        }
    }

    // the model backing this card
    lateinit var playlist: Playlist
    // Listener for the track rows
    private lateinit var onSwipeListener: RecyclerTouchListener.OnSwipeListener
    private lateinit var onTouchListener: RecyclerTouchListener
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
//                    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                            fab_button,
//                            ViewCompat.getTransitionName(fab_button)
//                    ).toBundle()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Postpone the enter transition until image is loaded
        postponeEnterTransition()

        setContentView(R.layout.activity_playlistcard)

        // load the parceled item info (image, text etc)
        playlist = intent.getParcelableExtra(EXTRA_PLAYLIST_ITEM)

        // inject AFTER parsing out the model
        AndroidInjection.inject(this)

        onSwipeListener = createOnSwipeListener()
        onTouchListener = createOnTouchListener(onSwipeListener)

        // bind the viewmodel, passing through to the subviews
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PlaylistCardViewModel::class.java)
        initViewModel(viewModel)
        playlist_card_widget.loadPlaylist(playlist, onFabMenuSelectedListener, onSwipeListener, onTouchListener,this)

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
    fun startSwipingTracks(reswipeAll: Boolean = false) {
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

    private fun render(state: PlaylistCardViewModel.PlaylistCardViewState) {
        playlist_card_widget.render(state)
    }

    private fun intents(): Observable<out CardIntentMarker> {
        return Observable.merge(playlist_card_widget.intents(), eventsPublisher)
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
                        animatedView.animateBars()
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
                val animatedView = foregroundView.findViewById<EqualizerView>(R.id.equalizer_animation)
                animatedView.stopBars()
            }
        }
    }

    private val eventsPublisher: PublishSubject<CardIntentMarker> by lazy { PublishSubject.create<CardIntentMarker>() }

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
                        R.id.like_icon_container -> {
                            // send off like command
                            val model = viewModel.states().value?.stashedTracksList?.get(position)
                            message += "Liked: ${model?.name}: ${model?.liked}"
                            // only update if not already liked +
                            if (model?.liked == false) {
                                val newModel = model.copy()
                                newModel.pref = TrackModel.Pref.LIKED
                                eventsPublisher.onNext(TrackIntent.ChangeTrackPref.like(newModel))
                                playlist_card_widget.updateTrackPref(newModel, position)
                            }
                        }
                        R.id.dislike_icon_container -> {
                            val model = viewModel.states().value?.stashedTracksList?.get(position)
                            message += "Disliked: ${model?.name}: ${model?.disliked}"
                            // only update if not already disliked
                            if (model?.liked == true) {
                                val newModel = model.copy()
                                newModel.pref = TrackModel.Pref.DISLIKED
                                eventsPublisher.onNext(TrackIntent.ChangeTrackPref.dislike(newModel))
                                playlist_card_widget.updateTrackPref(newModel, position)
                            }
                        }
                    }
                    Utils.mLog(TAG, message)
                }

        return onTouchListener
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
        tracks_recycler_view.addOnItemTouchListener(onTouchListener)
    }

    override fun onPause() {
        super.onPause()
        tracks_recycler_view.removeOnItemTouchListener(onTouchListener)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        val b = outState ?: Bundle()
        b.putParcelable(EXTRA_PLAYLIST_ITEM, playlist)
        super.onSaveInstanceState(b)
    }

}