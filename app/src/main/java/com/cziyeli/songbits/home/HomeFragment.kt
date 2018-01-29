package com.cziyeli.songbits.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.songbits.R
import com.cziyeli.songbits.oldhome.OldHomeIntent
import dagger.android.support.AndroidSupportInjection
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

//    private val listener: PlaylistSection.ClickListener = object : PlaylistSection.ClickListener {
//        override fun onItemClick(view: View, item: PlaylistItem) {
//            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,
//                    view.findViewById(R.id.playlist_image),
//                    ViewCompat.getTransitionName(view.findViewById(R.id.playlist_image))
//            ).toBundle()
//
//            val intent = Intent(activity, PlaylistCardActivity::class.java)
//            intent.putExtra(EXTRA_PLAYLIST_ITEM, item)
//            startActivity(intent, bundle)
//        }
//    }
//    val PLAYLIST_SECTION_ONE = createSectionOne(listener)
//    val PLAYLIST_SECTION_TWO = createSectionTwo(listener)

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

    private fun setUpSectionedAdapter(view: View, savedInstanceState: Bundle?) {
        sectionAdapter = SectionedRecyclerViewAdapter()

        // add the dummy sections
//        sectionAdapter.addSection(PLAYLIST_SECTION_ONE)
//        sectionAdapter.addSection(PLAYLIST_SECTION_TWO)

        mLayoutManager = GridLayoutManager(context, 3)
        mLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (sectionAdapter.getSectionItemViewType(position)) {
                    SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> 3
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
        Utils.mLog(TAG, "render!!", "$state")
        // render subviews
//        playlistsAdapter.render(state)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    companion object {

        fun create(args: Bundle? = Bundle()) : HomeFragment {
            val fragment = HomeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}