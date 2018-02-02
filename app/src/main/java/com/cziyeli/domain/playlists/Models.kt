package com.cziyeli.domain.playlists

import android.os.Parcel
import android.os.Parcelable
import com.cziyeli.commons.Utils.createParcel
import com.cziyeli.domain.SimpleImage
import kaaes.spotify.webapi.android.models.PlaylistSimple
import kaaes.spotify.webapi.android.models.PlaylistTracksInformation

/**
 * Domain model wrapping a [PlaylistSimple] from the api =>
 * pass to [PlaylistsResult].
 */
data class Playlist(val id: String,
                    val name: String,
                    private val href: String,
                    private val uri: String,
                    val owner: Owner,
                    private val images: List<SimpleImage>?,
                    val tracksInfo: PlaylistTracksInformation?) : Parcelable {

    // num [Track]s in this playlist
    val totalTracksCount: Int
        get() = tracksInfo?.total ?: 0

    // cover image url
    val imageUrl: String?
        get() = images?.get(0)?.url

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable(Owner::class.java.classLoader),
            parcel.createTypedArrayList(SimpleImage),
            parcel.readParcelable(PlaylistTracksInformation::class.java.classLoader)) {
    }

    companion object {
        @JvmField
        val CREATOR = createParcel { Playlist(it) }

        // factory method to create from api model
        fun create(apiModel: PlaylistSimple) : Playlist {
            val owner = Owner(apiModel.owner.id, apiModel.owner.uri, apiModel.owner.display_name)
            val images = apiModel.images?.map { SimpleImage(it.height, it.width, it.url) }
            return Playlist(apiModel.id, apiModel.name, apiModel.href, apiModel.uri, owner, images, apiModel.tracks)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(href)
        parcel.writeString(uri)
        parcel.writeParcelable(owner, flags)
        parcel.writeTypedList(images)
        parcel.writeParcelable(tracksInfo, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}

/**
 * Owner of a [Playlist].
 */
data class Owner(val id: String, private val uri: String, val display_name: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(uri)
        parcel.writeString(display_name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Owner> {
        override fun createFromParcel(parcel: Parcel): Owner {
            return Owner(parcel)
        }

        override fun newArray(size: Int): Array<Owner?> {
            return arrayOfNulls(size)
        }
    }
}