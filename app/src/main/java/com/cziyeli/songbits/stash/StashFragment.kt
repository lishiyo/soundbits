package com.cziyeli.songbits.stash

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.songbits.R
import com.cziyeli.songbits.root.RootIntent
import com.cziyeli.songbits.root.RootViewState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_stash.*

/**
 * Curation tab with tracks.
 * Passes events to RootVM
 */
class StashFragment : Fragment(), MviView<RootIntent, RootViewState> {
    private val TAG = StashFragment::class.simpleName

    // intents
    private val eventsPublisher: PublishRelay<RootIntent> by lazy {
        PublishRelay.create<RootIntent>()
    }

    override fun intents(): Observable<out RootIntent> {
       return eventsPublisher
    }

    override fun render(state: RootViewState) {
        // re-render subviews given new state
        Utils.mLog(TAG, "RENDER", "$state")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_stash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // load all the cards
        initCards()
    }

    private fun initCards() {
        // likes
        likes_card.load("likes", mutableListOf(), null, null, null)

        // dislikes

        // recommended

        // top tracks
    }

    companion object {
        fun create(args: Bundle? = Bundle()) : StashFragment {
            val fragment = StashFragment()
            fragment.arguments = args
            return fragment
        }
    }
}