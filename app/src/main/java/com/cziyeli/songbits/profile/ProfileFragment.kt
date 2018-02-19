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
import com.cziyeli.domain.user.ProfileActionProcessor
import com.cziyeli.songbits.R
import com.cziyeli.songbits.root.RootActivity
import com.cziyeli.songbits.stash.StashFragment
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

class ProfileFragment : Fragment(), MviView<ProfileIntent, ProfileViewModel.ViewState> {
    private val TAG = StashFragment::class.simpleName

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var actionProcessor: ProfileActionProcessor

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
//        initCards()

        // fire fetch events
//        eventsPublisher.accept(StashIntent.InitialLoad())
    }


    override fun render(state: ProfileViewModel.ViewState) {
        Utils.mLog(TAG, "RENDER", "$state")
    }

    override fun intents(): Observable<out ProfileIntent> {
        return eventsPublisher
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