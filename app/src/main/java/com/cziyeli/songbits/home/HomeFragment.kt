package com.cziyeli.songbits.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.cziyeli.songbits.home.detail.PlaylistCardActivity
import com.cziyeli.songbits.home.detail.PlaylistCardActivity.Companion.EXTRA_PLAYLIST_ITEM
import com.cziyeli.songbits.oldhome.OldHomeIntent
import dagger.android.support.AndroidSupportInjection
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject


class HomeFragment : Fragment(), MviView<OldHomeIntent, HomeViewState> {
    private val TAG = HomeFragment::class.simpleName

    @Inject lateinit var api: SpotifyApi
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    // view models
    private lateinit var viewModel: HomeViewModel

    // views
    private lateinit var sectionAdapter: SectionedRecyclerViewAdapter
    private lateinit var mLayoutManager: GridLayoutManager

    // intents
    private val mLoadPublisher = PublishSubject.create<OldHomeIntent.LoadPlaylists>()

    // adapter
    private lateinit var PLAYLIST_RECENT: PlaylistSection
    private lateinit var PLAYLIST_FEATURED: PlaylistSection
    private lateinit var PLAYLIST_RECOMMENDED: PlaylistSection
    private val listener: PlaylistSection.ClickListener = object : PlaylistSection.ClickListener {
        override fun onFooterClick(section: PlaylistSection) {
            mLoadPublisher.onNext(OldHomeIntent.LoadPlaylists(offset = section.contentItemsTotal + 1))
        }

        override fun onItemClick(view: View, item: Playlist) {
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,
                    view.findViewById(R.id.playlist_image),
                    ViewCompat.getTransitionName(view.findViewById(R.id.playlist_image))
            ).toBundle()

            // todo
//            context?.startActivity(CardsActivity.create(context as Context, item))
            val intent = Intent(activity, PlaylistCardActivity::class.java)
            intent.putExtra(EXTRA_PLAYLIST_ITEM, item)
            startActivity(intent, bundle)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set up the views
        setUpSectionedAdapter(view, savedInstanceState)

        // bind the view model after all views are done
        initViewModel()

        // fetch the playlists
        mLoadPublisher.onNext(OldHomeIntent.LoadPlaylists())
    }

    private fun showLoading(vararg sections: Section) {
        // switch to loading
        sections.forEach { it.state = Section.State.LOADING }
        sectionAdapter.notifyDataSetChanged()
    }

    private fun setUpSectionedAdapter(view: View, savedInstanceState: Bundle?) {
        sectionAdapter = SectionedRecyclerViewAdapter()

        // add the sections (to fill in later)
        PLAYLIST_RECENT =  PlaylistSection(getString(R.string.playlist_section_recent), mutableListOf(), listener)
        PLAYLIST_FEATURED =  PlaylistSection(getString(R.string.playlist_section_featured), mutableListOf(), listener)
        PLAYLIST_RECOMMENDED =  PlaylistSection(getString(R.string.playlist_section_recommended), mutableListOf(), listener)
        sectionAdapter.addSection(PLAYLIST_RECENT)
        sectionAdapter.addSection(PLAYLIST_RECOMMENDED)
        sectionAdapter.addSection(PLAYLIST_FEATURED)

        mLayoutManager = GridLayoutManager(context, PLAYLISTS_COLUMN_COUNT)
        mLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (sectionAdapter.getSectionItemViewType(position)) {
                    SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> PLAYLISTS_COLUMN_COUNT
                    else -> 1
                }
            }
        }
        playlist_recyclerview.layoutManager = mLayoutManager
        playlist_recyclerview.adapter = sectionAdapter
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

        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())
    }

    override fun intents(): Observable<out OldHomeIntent> {
       return mLoadPublisher
    }

    override fun render(state: HomeViewState) {
        Utils.mLog(TAG,"RENDER", "status: ${state.status}, playlistCount: ${state.playlists.size}")
        when {
            // TODO this is assuming everything is for PLAYLIST_RECENT
            state.status == MviViewState.Status.SUCCESS && state.playlists.isNotEmpty() -> {
                val currentCount = Math.max(0, PLAYLIST_RECENT.contentItemsTotal)
                val newPlaylists = state.playlists.subList(currentCount, state.playlists.size)

                PLAYLIST_RECENT.addPlaylists(newPlaylists)
                PLAYLIST_RECENT.state = Section.State.LOADED
                sectionAdapter.notifyDataSetChanged()
            }
            state.status == MviViewState.Status.SUCCESS && state.playlists.isEmpty() -> {
                // todo show empty state
                Utils.mLog(TAG, "RENDER", "successful but empty")
            }
            state.status == MviViewState.Status.LOADING -> {
                showLoading(PLAYLIST_RECENT)
            }
            state.status == MviViewState.Status.ERROR -> {
                // todo show error state
                Utils.mLog(TAG, "RENDER", "error")
            }
        }

    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    companion object {
        const val PLAYLISTS_COLUMN_COUNT = 2

        fun create(args: Bundle? = Bundle()) : HomeFragment {
            val fragment = HomeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}