package com.cziyeli.songbits.profile

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.commons.errorToast
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.user.ProfileResult
import com.cziyeli.domain.user.UserResult
import com.cziyeli.songbits.MainActivity
import com.cziyeli.songbits.R
import com.cziyeli.songbits.base.ChipsIntent
import com.cziyeli.songbits.base.ChipsIntentMarker
import com.cziyeli.songbits.stash.SimpleCardViewModel
import com.hlab.fabrevealmenu.listeners.OnFABMenuSelectedListener
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.widget_expandable_chips.*
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import org.jetbrains.anko.intentFor
import javax.inject.Inject



class ProfileFragment : Fragment(), MviView<ProfileIntentMarker, ProfileViewModel.ViewState> {
    private val TAG = ProfileFragment::class.simpleName

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var simpleCardActionProcessor: SimpleCardActionProcessor

    @Inject
    lateinit var userManager: com.cziyeli.domain.user.UserManager

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

        // basic view setting
        val userImage = userManager.getCurrentUser().cover_image
        Glide.with(context).load(userImage).into(profile_avatar)
        val randomGenresLabel = SpannableString(resources.getString(R.string.action_random_genres))
        randomGenresLabel.setSpan(UnderlineSpan(), 0, randomGenresLabel.length, 0)
        action_randomize_seeds.text = randomGenresLabel

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

        // init click listeners = fetch recommended based on current stats
        action_randomize_seeds.setOnClickListener {
            chips_widget.showOrHideGenres()
            eventsPublisher.accept(ChipsIntent.PickRandomGenres())
        }
        action_get_recommended.setOnClickListener {
            val attrs = viewModel.currentTargetStats.convertToOutgoing()
            eventsPublisher.accept(ProfileIntent.FetchRecommendedTracks(seedGenres = chips_widget.getCurrentSelected(), attributes = attrs))
        }
        action_reset.setOnClickListener {
            eventsPublisher.accept(ProfileIntent.Reset())
        }
        logout.setOnClickListener { _ ->
            eventsPublisher.accept(ProfileIntent.LogoutUser())
        }

        if (userVisibleHint && isAdded) {
            fetchData()
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
            state.status == MviViewState.Status.SUCCESS && state.lastResult is UserResult.ClearUser -> {
                // kick back out to landing page!
                startActivity(context?.intentFor<MainActivity>())
            }
            state.status == MviViewState.Status.ERROR -> {
                "${state.error}".errorToast(context!!)
            }
            state.isFetchStatsSuccess() || (state.status == MviViewState.Status.SUCCESS && state.lastResult is ProfileResult.Reset) -> {
                stats_container_left.loadTrackStats(state.originalStats!!)
                stats_container_right.loadTrackStats(state.originalStats)
                if (state.lastResult is ProfileResult.Reset) {
                    chips_widget.showOrHideGenres(false) // collapse it
                }

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
        chips_widget.processIntents(eventsPublisher.ofType(ChipsIntentMarker::class.java))
    }

    override fun intents(): Observable<out ProfileIntentMarker> {
        return eventsPublisher
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser && isAdded) {
            fetchData()
        }
    }

    override fun onResume() {
        super.onResume()

        recommended_tracks_card?.onResume()
        if (userVisibleHint && isAdded) {
            fetchData()
        }
    }

    /**
     * Load the data for the screen - should only happen when visible.
     */
    private fun fetchData() {
        // fetch initial stats (of liked)
        eventsPublisher.accept(ProfileIntent.LoadTracksForOriginalStats())
        // fetchData subviews
        eventsPublisher.accept(ChipsIntent.FetchSeedGenres())
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