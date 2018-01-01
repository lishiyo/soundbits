package com.cziyeli.domain.playlists

import kaaes.spotify.webapi.android.models.PlaylistSimple

/**
 * Domain model wrapping {@link PlaylistSimple}
 * Created by connieli on 12/31/17.
 */
data class Playlist(val id: String, val name: String, val href: String, val uri: String, val owner: Owner?, val images: List<Image>) {

    companion object {
        // create from api model
        fun create(apiModel: PlaylistSimple) : Playlist {
            var owner: Owner? = null
            apiModel.owner?.let {
                owner = Owner(apiModel.owner.id, apiModel.owner.uri, apiModel.owner.display_name)
            }
            val images = apiModel.images.map { Image(it.height, it.width, it.url) }
            return Playlist(apiModel.id, apiModel.name, apiModel.href, apiModel.uri, owner, images)
        }
    }
}

data class Image(val height: Int?, val width: Int?, val url: String)

data class Owner(val id: String, val uri: String, val display_name: String? = null)