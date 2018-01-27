package com.cziyeli.songbits.oldhome

import com.cziyeli.commons.Utils
import com.cziyeli.songbits.R
import com.mindorks.placeholderview.InfinitePlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.infinite.LoadMore
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.commons.mvibase.MviViewState


/**
 * Binds the placholder view with the data.
 *
 * Created by connieli on 1/1/18.
 */
@Layout(R.layout.load_more_view)
class InfinitePlaylistsAdapter(private val mLoadMoreView: InfinitePlaceHolderView) : MviView<HomeIntent, HomeViewState> {
    private val TAG = InfinitePlaylistsAdapter::class.simpleName
    private val mLoadPublisher = PublishSubject.create<HomeIntent.LoadPlaylists>()

    override fun render(state: HomeViewState) {
        var reachedEnd = state.status != MviViewState.Status.LOADING && state.status != MviViewState.Status.SUCCESS
        if (state.status == MviViewState.Status.SUCCESS) {
            if (state.playlists.isNotEmpty()) {
                val currentCount = Math.max(0, mLoadMoreView.viewCount)
                val playlists = state.playlists
                val newPlaylists = playlists.subList(currentCount, playlists.size)

                newPlaylists.forEach{
                    mLoadMoreView.addView<PlaylistItemView>(PlaylistItemView(mLoadMoreView.context, it))
                }

                mLoadMoreView.loadingDone() // finished populating adapter
            } else {
                // if successful and allTracks are empty, then we've reached end
                reachedEnd = true
            }
        }

        if (reachedEnd) { // error, not-logged-in
            Utils.log(TAG,"playlistsAdapter RENDER ++ reached end with state: ${state.status}")
            mLoadMoreView.noMoreToLoad()
        }
    }

    override fun intents(): Observable<out HomeIntent> {
        return mLoadPublisher
    }

    @LoadMore
    private fun onLoadMore() {
        Utils.log(TAG, "playlistAdapter onLoadMore ++ currentCount: ${mLoadMoreView.viewCount}")

        // post intent to load playlists
        mLoadPublisher.onNext(HomeIntent.LoadPlaylists(offset = mLoadMoreView.viewCount))
    }
}