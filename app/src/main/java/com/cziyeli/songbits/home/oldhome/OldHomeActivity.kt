package com.cziyeli.songbits.home.oldhome

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.songbits.R
import com.cziyeli.songbits.home.HomeIntent
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.activity_oldhome.*
import javax.inject.Inject


/**
 * Main screen:
 * - Show user's allTracks
 * - show current liked and discard piles
 *
 * Created by connieli on 12/31/17.
 */
class OldHomeActivity : AppCompatActivity(), MviView<HomeIntent, HomeViewState> {
    private val TAG = OldHomeActivity::class.simpleName

    @Inject lateinit var api: SpotifyApi
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    // view models
    private lateinit var viewModelOld: OldHomeViewModel

    // subviews
    private lateinit var playlistsAdapter: InfinitePlaylistsAdapter

    // intents
    private val mLoadPublisher = PublishSubject.create<HomeIntent.LoadPlaylists>()

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dagger
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oldhome)

        // initWith the allTracks view
        playlistsAdapter = InfinitePlaylistsAdapter(playlists_container)
        playlists_container.setLoadMoreResolver(playlistsAdapter)

        // bind the view model after all views are done
        initViewModel()

        mLoadPublisher.onNext(HomeIntent.LoadPlaylists())
    }

    override fun intents(): Observable<out HomeIntent> {
        return Observable.merge(
                mLoadPublisher, // own load intent
                playlistsAdapter.intents() // subviews intents
        )
    }

    override fun render(state: HomeViewState) {
        // render subviews
        playlistsAdapter.render(state)
    }

    private fun initViewModel() {
        viewModelOld = ViewModelProviders.of(this, viewModelFactory).get(OldHomeViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModelOld.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states with LiveData, not Rx
        compositeDisposable.add(
                viewModelOld.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModelOld.processIntents(intents())
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

}