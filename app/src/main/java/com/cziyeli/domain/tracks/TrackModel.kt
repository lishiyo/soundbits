package com.cziyeli.domain.tracks

import com.cziyeli.domain.SimpleImage
import kaaes.spotify.webapi.android.models.Track

/**
 * Domain model wrapping [Track] from the api =>
 * pass to [TrackResult]
 *
 * Created by connieli on 1/1/18.
 */
data class TrackModel(val name: String,
                      val id: String,
                      val uri: String,
                      val preview_url: String?,
                      val album: TrackAlbum,
                      val is_playable: Boolean?,
                      val popularity: Int?,
                      var pref: Pref = Pref.UNSEEN
) {
    enum class Pref {
        LIKED, // user liked this track
        DISLIKED, // user discarded this track
        UNSEEN // user hasn't seen this yet
    }

    val simpleImage: SimpleImage?
        get() = (album.images?.get(0))

    val artist: Artist?
        get() = (album.artists?.get(0))


    /**
     * @return whether this can be shown in the tinder ui
     */
    fun isRenderable(): Boolean = preview_url != null

    companion object {
        fun create(apiModel: Track) : TrackModel {
            val artists: List<Artist> = apiModel.artists.map { Artist(it.name, it.id, it.uri) }
            val images = apiModel.album.images?.map { SimpleImage(it.height, it.width, it.url) }
            val trackAlbum = TrackAlbum(apiModel.album.id, apiModel.album.uri, artists, images)
            return TrackModel(
                    apiModel.name,
                    apiModel.id,
                    apiModel.uri,
                    apiModel.preview_url,
                    trackAlbum,
                    apiModel.is_playable,
                    apiModel.popularity
            )
        }
    }
}

data class TrackAlbum(val id: String, val uri: String, val artists: List<Artist>, val images: List<SimpleImage>?)

data class Artist(val name: String, val id: String, val uri: String)

