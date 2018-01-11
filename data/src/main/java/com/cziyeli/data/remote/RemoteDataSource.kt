package com.cziyeli.data.remote

import com.cziyeli.commons.Utils
import io.reactivex.Observable
import io.reactivex.Single
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.models.*
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import javax.inject.Inject

/**
 * Created by connieli on 12/31/17.
 */
class RemoteDataSource @Inject constructor(private val api: SpotifyApi,
                                           private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = RemoteDataSource::class.simpleName

    fun fetchCurrentUser() : Single<UserPrivate> {
        return Single.create<UserPrivate>( { emitter ->
            api.service.getMe(object : Callback<UserPrivate> {
                override fun failure(error: RetrofitError?) {
                    emitter.onError(Throwable(error?.localizedMessage))
                }

                override fun success(t: UserPrivate?, response: Response?) {
                    emitter.onSuccess(t)
                }
            })
        })
    }

    fun fetchUserPlaylists(limit: Int = 20, offset: Int = 0): Observable<Pager<PlaylistSimple>> {
        return Observable.create<Pager<PlaylistSimple>>({ emitter ->
            // get all the com.cziyeli.domain.tracks in a playlist
            val params = mapOf("limit" to limit, "offset" to offset)

            api.service.getMyPlaylists(params, object : Callback<Pager<PlaylistSimple>> {
                override fun success(pagedResponse: Pager<PlaylistSimple>?, response: Response?) {
                    emitter.onNext(pagedResponse)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.log(TAG, "fetch playlists error: ${error?.localizedMessage}")
                    emitter.onError(Throwable(error?.localizedMessage))
                }
            })
        })
    }

    fun fetchPlaylistTracks(ownerId: String,
                            playlistId: String,
                            fields: String?,
                            limit: Int = 100,
                            offset: Int = 0): Observable<Pager<PlaylistTrack>> {
        return Observable.create<Pager<PlaylistTrack>>({ emitter ->
            val params = mutableMapOf<String, Any>("limit" to limit, "offset" to offset)
            fields?.let {
                params.put("fields", fields)
            }

            api.service.getPlaylistTracks(ownerId, playlistId, params, object : Callback<Pager<PlaylistTrack>> {
                override fun success(pagedResponse: Pager<PlaylistTrack>?, response: Response?) {
                    emitter.onNext(pagedResponse)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.log(TAG, "api ++ getPlaylistTracks error: ${error?.localizedMessage}")
                    emitter.onError(Throwable(error?.localizedMessage))
                }
            })
        })
    }

    fun fetchTracksData(trackIds: List<String>) : Observable<AudioFeaturesTracks> {
        return Observable.create<AudioFeaturesTracks>({ emitter ->
            val params = trackIds.joinToString(separator = ",")
            api.service.getTracksAudioFeatures(params, object : Callback<AudioFeaturesTracks> {
                override fun success(resp: AudioFeaturesTracks?, response: Response?) {
                    Utils.mLog(TAG, "fetchTracksStats", "success!")
                    emitter.onNext(resp)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.mLog(TAG, "fetchTracksStats", "error", error?.localizedMessage)
                    emitter.onError(Throwable(error?.localizedMessage))
                }
            })
        })
    }

    fun createPlaylist(ownerId: String, name: String, description: String?, public: Boolean): Single<Playlist> {
        return Single.create<Playlist>({ emitter ->
            val params = mapOf("description" to description, "public" to public, "name" to name)
            api.service.createPlaylist(ownerId, params, object : Callback<Playlist> {
                override fun success(resp: Playlist?, response: Response?) {
                    Utils.mLog(TAG, "createPlaylist", "success!")
                    emitter.onSuccess(resp)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.mLog(TAG, "createPlaylist", "error", error?.localizedMessage)
                    emitter.onError(Throwable(error?.localizedMessage))
                }
            })
        })
    }

    fun addTracksToPlaylist(ownerId: String, playlistId: String, trackUris: List<String>): Observable<Pair<String, SnapshotId>> {
        return Observable.create<Pair<String, SnapshotId>>({ emitter ->
                val params = mapOf("position" to 0, "uris" to TrackData(trackUris))
                Utils.mLog(TAG, "addTracksToPlaylist!", "params", params.toString(), "trackdata: ", TrackData(trackUris).toString())
                val snapshotId: SnapshotId = api.service.addTracksToPlaylist(ownerId, playlistId, params, mapOf()) // sync request
                val pair = Pair(playlistId, snapshotId)
                emitter.onNext(pair)
        })
    }


//    fun addTracksToPlaylist(ownerId: String, playlistId: String, trackUris: List<String>): Observable<Pair<String, Pager<PlaylistTrack>>> {
//        return Observable.create<Pair<String, Pager<PlaylistTrack>>>({ emitter ->
//            val params = mapOf("position" to 0, "uris" to trackUris.joinToString())
//
//
////            api.service.addTracksToPlaylist(ownerId, playlistId, params, mapOf(), object : Callback<Pager<PlaylistTrack>> {
////                override fun success(resp: Pager<PlaylistTrack>, response: Response?) {
////                    Utils.mLog(TAG, "createPlaylist", "success!")
////                    val pair = Pair<String, Pager<PlaylistTrack>>(playlistId, resp)
////                    emitter.onNext(pair)
////                }
////
////                override fun failure(error: RetrofitError?) {
////                    Utils.mLog(TAG, "createPlaylist", "error", error?.localizedMessage)
////                    emitter.onError(Throwable(error?.localizedMessage))
////                }
////            })
//        })
//    }


}

