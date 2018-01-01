package com.cziyeli.data.remote

import com.cziyeli.commons.Utils
import io.reactivex.Observable
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import javax.inject.Inject

/**
 * Created by connieli on 12/31/17.
 */
class RemoteDataSource @Inject constructor(private val api: SpotifyApi, private val schedulerProvider: BaseSchedulerProvider) {

    fun fetchUserPlaylists(limit: Int = 20, offset: Int = 0): Observable<Pager<PlaylistSimple>> {
        return Observable.create<Pager<PlaylistSimple>>({ emitter ->
            // get all the tracks in a playlist
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
}

