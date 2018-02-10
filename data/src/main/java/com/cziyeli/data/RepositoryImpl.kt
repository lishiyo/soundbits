package com.cziyeli.data

import com.cziyeli.commons.Utils
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.data.local.TracksDatabase
import com.cziyeli.data.remote.RemoteDataSource
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Flowables
import kaaes.spotify.webapi.android.models.*
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
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

    override fun fetchCurrentUser(): Single<UserPrivate> {
        return remoteDataSource.fetchCurrentUser()
    }

    override fun fetchUserPlaylists(source: Repository.Source, limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return fetchUserPlaylistsRemote(limit, offset)
    }

    override fun fetchPlaylistTracks(source: Repository.Source, ownerId: String, playlistId: String, fields: String?, limit: Int, offset: Int):
            Observable<Pager<PlaylistTrack>> {
        return fetchPlaylistTracksRemote(ownerId, playlistId, fields, limit, offset)
    }

    override fun fetchPlaylistStashedTracks(source: Repository.Source, playlistId: String, fields: String?, limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getTracksByPlaylistId(playlistId).distinctUntilChanged()
    }

    // fetch the audio features for list of tracks
    override fun fetchTracksStats(source: Repository.Source, trackIds: List<String>) : Observable<AudioFeaturesTracks> {
        return fetchTracksDataRemote(trackIds)
    }

    override fun updateTrackPref(trackId: String, liked: Boolean) {
        Observable.just(trackId)
                .subscribeOn(SchedulerProvider.io())
                .doOnNext{ Utils.mLog(TAG, "updateTrackPref", "track: $trackId: $liked") }
                .subscribe {
                    tracksDatabase.tracksDao().updateTrackPref(trackId, liked)
                }
    }

    ///////////////////////
    // ====== LOCAL ======
    ///////////////////////

    override fun saveTracksLocal(tracks: List<TrackEntity>) {
        Observable.just(tracks)
                .subscribeOn(SchedulerProvider.io())
                .subscribe { tracksDatabase.tracksDao().saveTracks(it) }
    }

    override fun fetchUserQuickStats() : Flowable<Triple<Int, Int, Int>> {
        return Flowables.zip<Int, Int, Int, Triple<Int, Int, Int>>(
                tracksDatabase.tracksDao().getStashedTracksCount(),
                tracksDatabase.tracksDao().getStashedTracksLikedCount(),
                tracksDatabase.tracksDao().getStashedTracksDislikedCount(),
                { totalCount: Int, likedCount: Int, dislikedCount: Int ->
                    Triple(totalCount, likedCount, dislikedCount)
                }
        )
    }

    override fun fetchUserLikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getLikedTracks(limit, offset)
    }

    override fun fetchUserDislikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getDislikedTracks(limit, offset)
    }

    ///////////////////////
    // ====== REMOTE ======
    ///////////////////////

    override fun createPlaylist(ownerId: String, name: String, description: String?, public: Boolean): Single<Playlist> {
        return remoteDataSource.createPlaylist(ownerId, name, description, public)
    }

    override fun addTracksToPlaylist(ownerId: String, playlistId: String, trackUris: List<String>):
            Observable<Pair<String, SnapshotId>> {
        return remoteDataSource.addTracksToPlaylist(ownerId, playlistId, trackUris)
    }

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

    // logs database - otherwise can just inspect Stetho resources
    override fun debug(limit: Int) {
        tracksDatabase.tracksDao()
                .queryAllDistinct()
                .subscribeOn(SchedulerProvider.io())
                .subscribe({ tracks ->
                    val str = if (limit > 0) {
                        tracks.take(limit).joinToString("\n")
                    } else {
                        tracks.joinToString("\n")
                    }

                    Utils.mLog(TAG, "debug", "SUCCESS -- TOTAL: ${tracks.size} \n", str)
                }, {
                    Utils.mLog(TAG, "debug", "ERR \n", it.localizedMessage)
                })
    }

    companion object {
        val TAG = RepositoryImpl.javaClass.simpleName
    }
}