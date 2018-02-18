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
        return fetchUserPlaylistsRemote(limit, offset).distinctUntilChanged()
    }

    override fun fetchFeaturedPlaylists(source: Repository.Source, limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return fetchFeaturedPlaylistsRemote(limit, offset).distinctUntilChanged()
    }

    override fun fetchPlaylistTracks(source: Repository.Source, ownerId: String, playlistId: String, fields: String?, limit: Int, offset: Int):
            Observable<Pager<PlaylistTrack>> {
        return fetchPlaylistTracksRemote(ownerId, playlistId, fields, limit, offset).distinctUntilChanged()
    }

    // fetch the audio features for list of tracks
    override fun fetchTracksStats(source: Repository.Source, trackIds: List<String>) : Observable<AudioFeaturesTracks> {
        return fetchTracksDataRemote(trackIds).distinctUntilChanged()
    }

    override fun updateTrackPref(trackId: String, liked: Boolean) {
        Observable.just(trackId)
                .subscribeOn(SchedulerProvider.io())
                .doOnNext{ Utils.mLog(TAG, "updateTrackPref", "track: $trackId: $liked") }
                .subscribe {
                    tracksDatabase.tracksDao().updateTrackPref(trackId, liked)
                }
    }

    override fun fetchUserTopTracks(source: Repository.Source, time_range: String?, limit: Int, offset: Int) : Observable<Pager<Track>> {
        return remoteDataSource.fetchUserTopTracks(time_range, limit, offset)
    }

    ///////////////////////
    // ====== LOCAL ======
    ///////////////////////

    override fun fetchPlaylistStashedTracks(source: Repository.Source, playlistId: String, fields: String?, limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getVisibleTracksByPlaylistId(playlistId, limit, offset).distinctUntilChanged()
    }

    override fun fetchStashedTracksByIds(source: Repository.Source, trackIds: List<String>, fields: String?, limit: Int, offset: Int):
            Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getTracksForTrackIds(trackIds, limit, offset).distinctUntilChanged()
    }

    override fun saveTracksLocal(tracks: List<TrackEntity>) {
        Observable.just(tracks)
                .subscribeOn(SchedulerProvider.io())
                .subscribe { tracksDatabase.tracksDao().saveTracks(it) }
    }

    override fun fetchUserQuickStats() : Flowable<Triple<Int, Int, Int>> {
        return Flowables.combineLatest<Int, Int, Int, Triple<Int, Int, Int>>(
                tracksDatabase.tracksDao().getStashedTracksCount().distinctUntilChanged(),
                tracksDatabase.tracksDao().getStashedTracksLikedCount().distinctUntilChanged(),
                tracksDatabase.tracksDao().getStashedTracksDislikedCount().distinctUntilChanged(),
                { totalCount: Int, likedCount: Int, dislikedCount: Int ->
                    Triple(totalCount, likedCount, dislikedCount)
                }
        )
    }

    override fun fetchUserTracks(pref: Repository.Pref, limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return when (pref) {
            Repository.Pref.LIKED -> fetchUserLikedTracks(limit, offset).distinctUntilChanged()
            Repository.Pref.DISLIKED -> fetchUserDislikedTracks(limit, offset).distinctUntilChanged()
            else -> tracksDatabase.tracksDao().getVisibleTracks(limit, offset).distinctUntilChanged()
        }
    }

    override fun clearStashedTracks(pref: Repository.Pref) {
        Observable.just(pref)
                .subscribeOn(SchedulerProvider.io())
                .subscribe {
                    when (pref) {
                        Repository.Pref.LIKED -> tracksDatabase.tracksDao().clearTracks(true)
                        Repository.Pref.DISLIKED -> tracksDatabase.tracksDao().clearTracks(false)
                        else -> tracksDatabase.tracksDao().clearAllTracks()
                    }
                }
    }

    private fun fetchUserLikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getLikedTracks(limit, offset).distinctUntilChanged()
    }

    private fun fetchUserDislikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>> {
        return tracksDatabase.tracksDao().getDislikedTracks(limit, offset).distinctUntilChanged()
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

    private fun fetchFeaturedPlaylistsRemote(limit: Int, offset: Int): Observable<Pager<PlaylistSimple>> {
        return remoteDataSource.fetchFeaturedPlaylists(limit, offset).map { it.playlists }
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