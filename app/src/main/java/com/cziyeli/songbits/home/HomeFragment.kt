package com.cziyeli.songbits.home

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.playlists.PlaylistsResult
import com.cziyeli.domain.user.UserResult
import com.cziyeli.songbits.R
import com.cziyeli.songbits.playlistcard.PlaylistCardActivity
import com.cziyeli.songbits.root.RootActivity
import com.cziyeli.songbits.root.RootIntent
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.support.AndroidSupportInjection
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.widget_quickcounts_row.*
import javax.inject.Inject


class HomeFragment : Fragment(), MviView<HomeIntent, HomeViewState> {
    private val TAG = HomeFragment::class.simpleName

    @Inject
    lateinit var api: SpotifyApi
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var userManager: com.cziyeli.domain.user.UserManager

    // view models
    private lateinit var viewModel: HomeViewModel

    // views
    private lateinit var sectionAdapter: SectionedRecyclerViewAdapter
    private lateinit var mLayoutManager: GridLayoutManager

    // intents
    private val eventsPublisher: PublishRelay<HomeIntent> by lazy { PublishRelay.create<HomeIntent>() }
    private val compositeDisposable = CompositeDisposable()

    // adapter
    private lateinit var PLAYLIST_RECENT: PlaylistSection
    private lateinit var PLAYLIST_FEATURED: PlaylistSection
    private val recentListener: PlaylistSection.ClickListener = object : PlaylistSection.ClickListener {
        override fun onFooterClick(section: PlaylistSection) {
            eventsPublisher.accept(HomeIntent.LoadUserPlaylists(offset = section.contentItemsTotal + 1))
        }

        override fun onItemClick(view: View, item: Playlist) {
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,
                    view.findViewById(R.id.playlist_image),
                    ViewCompat.getTransitionName(view.findViewById(R.id.playlist_image))
            ).toBundle()

            startActivity(PlaylistCardActivity.create(context!!, item), bundle)
        }
    }

    private val featuredListener: PlaylistSection.ClickListener = object : PlaylistSection.ClickListener {
        override fun onFooterClick(section: PlaylistSection) {
            eventsPublisher.accept(HomeIntent.LoadFeaturedPlaylists(offset = section.contentItemsTotal + 1))
        }

        override fun onItemClick(view: View, item: Playlist) {
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,
                    view.findViewById(R.id.playlist_image),
                    ViewCompat.getTransitionName(view.findViewById(R.id.playlist_image))
            ).toBundle()

            startActivity(PlaylistCardActivity.create(context!!, item), bundle)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // bind the view model before events
        initViewModel()

        // set up the user mini card
        loadUserCard()

        // set up the views
        setUpSectionedAdapter(view, savedInstanceState)

        // fetch the playlists
        eventsPublisher.accept(HomeIntent.LoadUserPlaylists(limit = 11))
        eventsPublisher.accept(HomeIntent.LoadFeaturedPlaylists(limit = 11))
    }

    // the little mini card with the stats
    private fun loadUserCard() {
        // use root's publisher to process at the root level
        (activity as RootActivity).getRootPublisher().accept(RootIntent.FetchUserQuickCounts())

        val userName = userManager.getCurrentUser().display_name
        val userImage = userManager.getCurrentUser().cover_image

        user_card_name.text = userName
        Glide.with(context).load(userImage).into(user_card_image)
    }

    // show loading view for list of sections
    private fun showLoading(vararg sections: Section) {
        // switch to loading
        sections.forEach { it.state = Section.State.LOADING }
        sectionAdapter.notifyDataSetChanged()
    }

    private fun setUpSectionedAdapter(view: View, savedInstanceState: Bundle?) {
        sectionAdapter = SectionedRecyclerViewAdapter()

        // add the sections (to fill in later)
        PLAYLIST_RECENT = PlaylistSection(getString(R.string.playlist_section_recent), mutableListOf(), recentListener)
        PLAYLIST_FEATURED = PlaylistSection(getString(R.string.playlist_section_featured), mutableListOf(), featuredListener)
        sectionAdapter.addSection(PLAYLIST_RECENT)
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

    override fun intents(): Observable<out HomeIntent> {
        return eventsPublisher
    }

    override fun render(state: HomeViewState) {
        Utils.mLog(TAG, "RENDER", "$state")
        when {
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.UserPlaylists
                    && state.userPlaylists.isNotEmpty() -> {
                val currentCount = Math.max(0, PLAYLIST_RECENT.contentItemsTotal)
                val newPlaylists = state.userPlaylists.subList(currentCount, state.userPlaylists.size)

                PLAYLIST_RECENT.addPlaylists(newPlaylists.toMutableList())
                PLAYLIST_RECENT.state = Section.State.LOADED
                sectionAdapter.notifyDataSetChanged()
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.UserPlaylists
                    && state.userPlaylists.isEmpty() -> {
                // todo show empty state
                Utils.mLog(TAG, "RENDER", "no user playlists")
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.FeaturedPlaylists
                    && state.featuredPlaylists.isNotEmpty() -> {
                val currentCount = Math.max(0, PLAYLIST_FEATURED.contentItemsTotal)
                val newPlaylists = state.featuredPlaylists.subList(currentCount, state.featuredPlaylists.size)

                PLAYLIST_FEATURED.addPlaylists(newPlaylists.toMutableList())
                PLAYLIST_FEATURED.state = Section.State.LOADED
                sectionAdapter.notifyDataSetChanged()
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.FeaturedPlaylists
                    && state.featuredPlaylists.isEmpty() -> {
                // todo show empty state
                Utils.mLog(TAG, "RENDER", "no featured playlists")
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.UserPlaylists -> {
                showLoading(PLAYLIST_RECENT)
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is PlaylistsResult.FeaturedPlaylists -> {
                showLoading(PLAYLIST_FEATURED)
            }
            state.status == MviViewState.Status.ERROR -> {
                // todo show error state
                Utils.mLog(TAG, "RENDER", "error")
            }
        }

        if (state.status == MviViewState.Status.SUCCESS && state.lastResult is UserResult.FetchQuickCounts) {
            quickstats_likes.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_dislikes.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_total.setTextColor(resources.getColor(R.color.colorWhite))
            quickstats_likes.text = "${state.quickCounts?.likedCount} likes"
            quickstats_dislikes.text = "${state.quickCounts?.dislikedCount} dislikes"
            quickstats_total.text = "${state.quickCounts?.totalCount} swiped"
        }
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
        const val PLAYLISTS_COLUMN_COUNT = 2

        fun create(args: Bundle? = Bundle()): HomeFragment {
            val fragment = HomeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}