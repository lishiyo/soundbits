package com.cziyeli.songbits.stash

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.Repository
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.stash.StashResult
import com.cziyeli.domain.user.UserResult
import com.cziyeli.songbits.R
import com.cziyeli.songbits.root.RootActivity
import com.cziyeli.songbits.root.RootIntent
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_stash.*
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

/**
 * Curation tab with tracks.
 *
 * Passes events to RootVM
 */
class StashFragment : Fragment(), MviView<StashIntent, StashViewModel.ViewState> {
    private val TAG = StashFragment::class.simpleName

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var simpleCardActionProcessor: SimpleCardActionProcessor

    val schedulerProvider = SchedulerProvider
    private lateinit var viewModel: StashViewModel

    // intents
    private val eventsPublisher: PublishRelay<StashIntent> by lazy { PublishRelay.create<StashIntent>() }
    private val compositeDisposable = CompositeDisposable()

    // Likes/dislikes FAB menu
    private val likesFabMenuSelectedListener: OnFABMenuSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_clear -> {
                eventsPublisher.accept(StashIntent.ClearTracks(Repository.Pref.LIKED))
            }
            R.id.menu_create_playlist -> {
                likes_card.initCreateMode()
            }
        }
    }
    private val disLikesFabMenuSelectedListener: OnFABMenuSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_clear -> {
                eventsPublisher.accept(StashIntent.ClearTracks(Repository.Pref.DISLIKED))
            }
            R.id.menu_create_playlist -> {
                dislikes_card.initCreateMode()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_stash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        // load all the cards (empty for now)
        initCards()

        // fire fetch events
        eventsPublisher.accept(StashIntent.InitialLoad())
    }

    override fun intents(): Observable<out StashIntent> {
        return eventsPublisher
    }

    override fun render(state: StashViewModel.ViewState) {
        when {
            state.status == MviViewState.Status.SUCCESS && state.lastResult is UserResult.LoadLikesCard -> {
                likes_card.loadTracks(state.likedTracks)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is UserResult.LoadDislikesCard -> {
                dislikes_card.loadTracks(state.dislikedTracks)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is StashResult.FetchUserTopTracks -> {
                top_tracks_card.loadTracks(state.topTracks)
            }
        }
    }

    private fun initCards() {
        val activity = activity as RootActivity

        // likes
        likes_card.initWith(resources.getString(R.string.likes_card_title), mutableListOf(), activity,
                SimpleCardViewModel(
                        simpleCardActionProcessor,
                        schedulerProvider,
                        SimpleCardViewModel.ViewState()
                ),
                likesFabMenuSelectedListener
        )

        // dislikes
        dislikes_card.initWith(resources.getString(R.string.dislikes_card_title), mutableListOf(), activity,
                SimpleCardViewModel(
                        simpleCardActionProcessor,
                        schedulerProvider,
                        SimpleCardViewModel.ViewState()
                ),
                disLikesFabMenuSelectedListener
        )

        // top tracks
        top_tracks_card.initWith(resources.getString(R.string.top_tracks_card_title), mutableListOf(), activity,
                SimpleCardViewModel(
                        simpleCardActionProcessor,
                        schedulerProvider,
                        SimpleCardViewModel.ViewState()
                ),
                null)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(StashViewModel::class.java)

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

        // Bind ViewModel to root states stream to listen to global state changes
        viewModel.processRootViewStates((activity as RootActivity).getStates())
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        Utils.mLog(TAG, "onResume! visible: $userVisibleHint ")
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        Utils.mLog(TAG, "setUserVisibleHint -- visible: $isVisibleToUser")
        if (isVisibleToUser) {
            // fetch the tracks
            (activity as RootActivity).getRootPublisher().accept(RootIntent.LoadLikedTracks())
            (activity as RootActivity).getRootPublisher().accept(RootIntent.LoadDislikedTracks())
            eventsPublisher.accept(StashIntent.FetchUserTopTracks())

            likes_card.onResume()
            dislikes_card.onResume()
            top_tracks_card.onResume()
        }
    }

    override fun onPause() {
        super.onPause()

        likes_card.onPause()
        dislikes_card.onPause()
        top_tracks_card.onPause()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    companion object {
        fun create(args: Bundle? = Bundle()) : StashFragment {
            val fragment = StashFragment()
            fragment.arguments = args
            return fragment
        }
    }
}