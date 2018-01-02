package com.cziyeli.domain.tracks

import com.cziyeli.domain.CoverImage
import kaaes.spotify.webapi.android.models.Track

/**
 * Domain model wrapping {@link Track} from the api =>
 * pass to results
 *
 * Created by connieli on 1/1/18.
 */
data class TrackCard(val name: String,
                     val id: String,
                     val uri: String,
                     val preview_url: String?,
                     val album: TrackAlbum,
                     val is_playable: Boolean?,
                     val popularity: Int?
) {
    val coverImage: CoverImage?
        get() = (album.images?.get(0))

    val artist: Artist?
        get() = (album.artists?.get(0))

    /**
     * @return whether this can be shown in the tinder ui
     */
    fun isRenderable(): Boolean = preview_url != null

    companion object {
        fun create(apiModel: Track) : TrackCard {
            val artists: List<Artist> = apiModel.artists.map { Artist(it.name, it.id, it.uri) }
            val images = apiModel.album.images?.map { CoverImage(it.height, it.width, it.url) }
            val trackAlbum = TrackAlbum(apiModel.album.id, apiModel.album.uri, artists, images)
            return TrackCard(
                    apiModel.name,
                    apiModel.id,
                    apiModel.uri,
                    apiModel.preview_url,
                    trackAlbum,
                    apiModel.is_playable,
                    apiModel.popularity)
        }
    }
}

data class TrackAlbum(val id: String, val uri: String, val artists: List<Artist>, val images: List<CoverImage>?)

data class Artist(val name: String, val id: String, val uri: String)

