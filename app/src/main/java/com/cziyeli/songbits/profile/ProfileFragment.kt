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
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.songbits.R
import com.cziyeli.songbits.root.RootActivity
import com.cziyeli.songbits.root.RootIntent
import com.cziyeli.songbits.stash.SimpleCardViewModel
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
    private val eventsPublisher: PublishRelay<ProfileIntent> by lazy { PublishRelay.create<ProfileIntent>() }
    private val compositeDisposable = CompositeDisposable()

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
        stats_container_left.statsChangePublisher.subscribe {
            // "target_danceability" => 0.55
            eventsPublisher.accept(ProfileIntent.StatChanged(viewModel.currentTargetStats, it))
        }
        stats_container_right.statsChangePublisher.subscribe {
            // "target_danceability" => 0.55
            eventsPublisher.accept(ProfileIntent.StatChanged(viewModel.currentTargetStats, it))
        }

        // attempt to fetch initial stats (of liked)
        (activity as RootActivity).getRootPublisher().accept(RootIntent.LoadLikedTracks())

        action_get_recommended.setOnClickListener {
            // grab the current attributes
            Utils.mLog(TAG, "clicked! current: ${viewModel.currentTargetStats}")
        }
    }

    private fun getCurrentAttributesMap() {

    }

    private fun initCards() {
        recommended_tracks_card.initWith(resources.getString(R.string.recommended_tracks_card_title), mutableListOf(), activity!!,
                SimpleCardViewModel(
                        simpleCardActionProcessor,
                        schedulerProvider,
                        SimpleCardViewModel.ViewState(shouldRemoveTrack = { _, _ -> false })
                ),
                null)
    }

    override fun render(state: ProfileViewModel.ViewState) {
        Utils.mLog(TAG, "RENDER", "$state")

        when {
            state.isFetchStatsSuccess() -> {
                stats_container_left.loadTrackStats(state.originalStats!!)
                stats_container_right.loadTrackStats(state.originalStats)
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

        // Bind ViewModel to root states stream to listen to global state changes
        viewModel.processRootViewStates((activity as RootActivity).getStates())
    }


    override fun intents(): Observable<out ProfileIntent> {
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

    companion object {
        fun create(args: Bundle? = Bundle()) : ProfileFragment {
            val fragment = ProfileFragment()
            fragment.arguments = args
            return fragment
        }
    }
}