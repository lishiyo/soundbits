package com.cziyeli.songbits.oldhome

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.songbits.R
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject


/**
 * Main screen:
 * - Show user's allTracks
 * - show current liked and discard piles
 *
 * Created by connieli on 12/31/17.
 */
class HomeActivity : AppCompatActivity(), MviView<HomeIntent, HomeViewState> {
    private val TAG = HomeActivity::class.simpleName

    @Inject lateinit var api: SpotifyApi
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    // view models
    private lateinit var viewModel: HomeViewModel

    // subviews
    private lateinit var playlistsAdapter: InfinitePlaylistsAdapter

    // intents
    private val mLoadPublisher = PublishSubject.create<HomeIntent.LoadPlaylists>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dagger
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(this, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())
    }

}