package com.cziyeli.data

import com.cziyeli.commons.Utils
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.data.local.TracksDatabase
import com.cziyeli.data.remote.RemoteDataSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple
import kaaes.spotify.webapi.android.models.PlaylistTrack
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class RepositoryImpl @Inject constructor(
        private val tracksDatabase: TracksDatabase,
        private val remoteDataSource: RemoteDataSource
) : Repository {
    val allCompositeDisposable: MutableList<Disposable> = arrayListOf()

    override fun fetchUserPlaylists(source: Repository.Source, limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return fetchUserPlaylistsRemote(limit, offset)
    }

    override fun fetchPlaylistTracks(source: Repository.Source, ownerId: String, playlistId: String, fields: String?, limit: Int, offset: Int):
            Observable<Pager<PlaylistTrack>> {
        return fetchPlaylistTracksRemote(ownerId, playlistId, fields, limit, offset)
    }

    // fetch the audio features for list of tracks
    override fun fetchTracksStats(source: Repository.Source, trackIds: List<String>) : Observable<AudioFeaturesTracks> {
        return fetchTracksDataRemote(trackIds)
    }

    ///////////////////////
    // ====== LOCAL ======
    ///////////////////////

    override fun saveTracksLocal(tracks: List<TrackEntity>) : Single<List<Long>> {
        val inserted = tracksDatabase.tracksDao().saveTracks(tracks)
        return if (inserted.isNotEmpty()) Single.just(inserted) else Single.error(Throwable("nothing inserted to db"))
    }

    ///////////////////////
    // ====== REMOTE ======
    ///////////////////////

    private fun fetchUserPlaylistsRemote(limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return remoteDataSource.fetchUserPlaylists(limit, offset)
    }

    private fun fetchPlaylistTracksRemote(ownerId: String, playlistId: String, fields: String?, limit: Int, offset: Int):
            Observable<Pager<PlaylistTrack>> {
        return remoteDataSource.fetchPlaylistTracks(ownerId, playlistId, fields, limit, offset)
    }

    private fun fetchTracksDataRemote(trackIds: List<String>) : Observable<AudioFeaturesTracks> {
        return remoteDataSource.fetchTracksData(trackIds)
    }


    ///////////////////////
    // ====== DEBUG ======
    ///////////////////////

    override fun debug(limit: Int) {
        tracksDatabase.tracksDao().queryAll().subscribe({ tracks ->
            val str = if (limit > 0) {
                tracks.take(limit).joinToString(",")
            } else {
                tracks.joinToString(",")
            }

            Utils.mLog(TAG, "dumpDatabase", "SUCCESS", str)
        }, {
            Utils.mLog(TAG, "dumpDatabase", "ERR", it.localizedMessage)
        })
    }

    companion object {
        val TAG = RepositoryImpl.javaClass.simpleName
    }
}