package com.cziyeli.songbits.home

import com.cziyeli.commons.Utils
import com.cziyeli.songbits.R
import com.mindorks.placeholderview.InfinitePlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.infinite.LoadMore
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.mvibase.MviView


/**
 * Binds the placholder view with the data.
 *
 * Created by connieli on 1/1/18.
 */
@Layout(R.layout.load_more_view)
class InfinitePlaylistsAdapter(private val mLoadMoreView: InfinitePlaceHolderView) : MviView<HomeIntent, HomeViewState> {

    private val mLoadPublisher = PublishSubject.create<HomeIntent.LoadPlaylists>()

    override fun render(state: HomeViewState) {
        Utils.log("InfinitePlaylists render: ${state.status}")
        var reachedEnd = state.status != HomeViewState.Status.LOADING && state.status != HomeViewState.Status.SUCCESS
        if (state.status == HomeViewState.Status.SUCCESS) {
            if (state.playlists.isNotEmpty()) {
                val currentCount = Math.max(0, mLoadMoreView.viewCount)
                val playlists = state.playlists
                val newPlaylists = playlists.subList(currentCount, playlists.size)

                Utils.log("playlistAdapter RENDER ++ currentCount: $currentCount ++ got newPlaylists: ${newPlaylists!!.size}")

                newPlaylists.forEach{
                    mLoadMoreView.addView<PlaylistItem>(PlaylistItem(mLoadMoreView.context, it))
                }

                mLoadMoreView.loadingDone() // finished populating adapter
            } else {
                // if successful and items are empty, then we've reached end
                reachedEnd = true
            }
        }

        if (reachedEnd) { // error, not-logged-in
            Utils.log("playlistsAdapter RENDER ++ reached end with state: ${state.status}")
            mLoadMoreView.noMoreToLoad()
        }
    }

    override fun intents(): Observable<out HomeIntent> {
        return mLoadPublisher
    }

    @LoadMore
    private fun onLoadMore() {
        Utils.log("playlistAdapter onLoadMore ++ currentCount: ${mLoadMoreView.viewCount}")

        // post intent to load items
        mLoadPublisher.onNext(HomeIntent.LoadPlaylists.create(offset = mLoadMoreView.viewCount))
    }
}