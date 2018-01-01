package com.cziyeli.data

import com.cziyeli.data.local.RoomDataSource
import com.cziyeli.data.remote.RemoteDataSource
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class RepositoryImpl @Inject constructor(
        private val roomDataSource: RoomDataSource,
        private val remoteDataSource: RemoteDataSource
) : Repository {

    val allCompositeDisposable: MutableList<Disposable> = arrayListOf()

    override fun fetchUserPlaylists(limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return remoteDataSource.fetchUserPlaylists(limit, offset)
    }


}