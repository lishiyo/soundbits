package com.cziyeli.data.remote

import com.cziyeli.commons.Utils
import io.reactivex.Observable
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple
import kaaes.spotify.webapi.android.models.PlaylistTrack
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

    fun fetchUserPlaylists(limit: Int = 20, offset: Int = 0): Observable<Pager<PlaylistSimple>> {
        return Observable.create<Pager<PlaylistSimple>>({ emitter ->
            // get all the com.cziyeli.domain.tracks in a playlist
            val params = mapOf("limit" to limit, "offset" to offset)

            api.service.getMyPlaylists(params, object : Callback<Pager<PlaylistSimple>> {
                override fun success(pagedResponse: Pager<PlaylistSimple>?, response: Response?) {
                    Utils.log("got playlists! total: ${pagedResponse?.total}")
                    emitter.onNext(pagedResponse)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.log("fetch playlists error: ${error?.localizedMessage}")
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
                    Utils.log("api ++ getPlaylistTracks SUCCESS ++ total: ${pagedResponse?.total}")
                    emitter.onNext(pagedResponse)
                }

                override fun failure(error: RetrofitError?) {
                    Utils.log("api ++ getPlaylistTracks error: ${error?.localizedMessage}")
                    emitter.onError(Throwable(error?.localizedMessage))
                }
            })
        })
    }
}

