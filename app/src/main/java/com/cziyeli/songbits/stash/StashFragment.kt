package com.cziyeli.songbits.stash

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.songbits.R
import kotlinx.android.synthetic.main.fragment_stash.*

class StashFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_stash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCards()
    }

    fun loadCards() {
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