package com.cziyeli.songbits.profile

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.user.ProfileActionProcessor
import com.cziyeli.domain.user.ProfileResultMarker
import com.cziyeli.songbits.root.RootViewState
import com.cziyeli.songbits.stash.StashViewModel
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Created by connieli on 2/18/18.
 */

class ProfileViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: ProfileActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<ProfileIntent, ProfileViewModel.ViewState> {
    private val TAG = StashViewModel::class.java.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for home-specific events
    private val intentsSubject : PublishRelay<ProfileIntent> by lazy { PublishRelay.create<ProfileIntent>() }
    // Listener for root view states
    private val rootStatesSubject: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<ProfileViewModel.ViewState> by lazy { PublishRelay.create<ProfileViewModel.ViewState>() }


    init {

    }

    override fun processIntents(intents: Observable<out ProfileIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    /**
     * Bind to the root stream.
     */
    fun processRootViewStates(intents: Observable<RootViewState>) {
        compositeDisposable.add(
                intents.subscribe(rootStatesSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
        return viewStates
    }

    data class ViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: ProfileResultMarker? = null
    ) : MviViewState
}