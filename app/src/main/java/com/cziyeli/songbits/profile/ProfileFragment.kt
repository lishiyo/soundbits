package com.cziyeli.songbits.profile

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.errorToast
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.user.ProfileResult
import com.cziyeli.songbits.R
import com.cziyeli.songbits.base.ChipsIntent
import com.cziyeli.songbits.stash.SimpleCardViewModel
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_profile.*
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

class ProfileFragment : Fragment(), MviView<ProfileIntentMarker, ProfileViewModel.ViewState> {
    private val TAG = ProfileFragment::class.simpleName

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var simpleCardActionProcessor: SimpleCardActionProcessor

    val schedulerProvider = SchedulerProvider
    private lateinit var viewModel: ProfileViewModel

    // intents
    private val eventsPublisher: PublishRelay<ProfileIntentMarker> by lazy { PublishRelay.create<ProfileIntentMarker>() }
    private val compositeDisposable = CompositeDisposable()

    // Listener for the FAB menu
    private val recommendedFABMenuSelectedListener = OnFABMenuSelectedListener { view, id ->
        when (id) {
            R.id.menu_surf -> {
                recommended_tracks_card.startSwipingTracks(false)
            }
            R.id.menu_resurf -> {
                recommended_tracks_card.startSwipingTracks(true)
            }
            R.id.menu_create_playlist -> {
                recommended_tracks_card.changeCreateMode()
            }
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        // load all the cards (empty for now)
        initCards()

        // bind to track changes
        compositeDisposable.addAll(
                stats_container_left.statsChangePublisher.subscribe {
                    // "target_danceability" => 0.55
                    eventsPublisher.accept(ProfileIntent.StatChanged(viewModel.currentTargetStats, it))
                },
                stats_container_right.statsChangePublisher.subscribe {
                    // "target_danceability" => 0.55
                    eventsPublisher.accept(ProfileIntent.StatChanged(viewModel.currentTargetStats, it))
                }
        )

        // attempt to fetch initial stats (of liked)
        eventsPublisher.accept(ProfileIntent.LoadTracksForOriginalStats())

        // init click listeners = fetch recommended based on current stats
        action_get_recommended.setOnClickListener {
            val attrs = viewModel.currentTargetStats.convertToOutgoing()
            eventsPublisher.accept(ProfileIntent.FetchRecommendedTracks(attributes = attrs))
        }
    }

    private fun initCards() {
        recommended_tracks_card.initWith(resources.getString(R.string.recommended_tracks_card_title), mutableListOf(), activity!!,
                SimpleCardViewModel(
                        simpleCardActionProcessor,
                        schedulerProvider,
                        SimpleCardViewModel.ViewState(shouldRemoveTrack = { _, _ -> false })
                ),
                recommendedFABMenuSelectedListener)
    }

    override fun render(state: ProfileViewModel.ViewState) {
        Utils.mLog(TAG, "RENDER", "$state")

        when {
            state.status == MviViewState.Status.ERROR -> {
                "oh no! something went wrong".errorToast(context!!)
            }
            state.isFetchStatsSuccess() -> {
                stats_container_left.loadTrackStats(state.originalStats!!)
                stats_container_right.loadTrackStats(state.originalStats)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ProfileResult.FetchRecommendedTracks -> {
                Utils.setVisible(recommended_tracks_card, true)
                recommended_tracks_card.loadTracks(state.recommendedTracks)
                // force-refresh for a new image
                recommended_tracks_card.refreshHeader(state.recommendedTracks)
            }
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ProfileViewModel::class.java)

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

        // Bind the subviews
        chips_widget.processIntents(eventsPublisher.ofType(ChipsIntent::class.java))
        eventsPublisher.accept(ChipsIntent.FetchSeedGenres())
    }

    override fun intents(): Observable<out ProfileIntentMarker> {
        return eventsPublisher
    }

    override fun onResume() {
        super.onResume()

        recommended_tracks_card?.onResume()
    }

    override fun onPause() {
        super.onPause()

        recommended_tracks_card?.onPause()
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    companion object {
        fun create(args: Bundle? = Bundle()) : ProfileFragment {
            val fragment = ProfileFragment()
            fragment.arguments = args
            return fragment
        }
    }
}