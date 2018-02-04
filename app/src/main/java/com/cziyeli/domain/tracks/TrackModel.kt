package com.cziyeli.domain.tracks

import android.os.Parcel
import android.os.Parcelable
import com.cziyeli.commons.Utils
import com.cziyeli.data.local.TrackEntity
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
                      private val album: TrackAlbum?,
                      val is_playable: Boolean?,
                      val popularity: Int?,
                      var pref: Pref = Pref.UNSEEN,
                      val artistName: String?,
                      val imageUrl: String?
) : Parcelable {
    enum class Pref : Parcelable {
        LIKED, // user liked this track
        DISLIKED, // user discarded this track
        UNSEEN; // user hasn't seen this yet

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(ordinal)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Pref> {
            override fun createFromParcel(`in`: Parcel): Pref {
                return Pref.values()[`in`.readInt()]
            }

            override fun newArray(size: Int): Array<Pref?> {
                return arrayOfNulls(size)
            }
        }
    }

    val liked: Boolean
        get() = pref == Pref.LIKED

    val disliked: Boolean
        get() = pref == Pref.DISLIKED

    /**
     * @return whether this can be shown in the tinder ui
     */
    val isSwipeable: Boolean = preview_url != null

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable(TrackAlbum::class.java.classLoader),
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readParcelable(Pref::class.java.classLoader),
            parcel.readString(),
            parcel.readString())

    companion object {

        @JvmField
        val CREATOR = Utils.createParcel { TrackModel(it) }

        fun createFromLocal(localModel: TrackEntity) : TrackModel {
            val pref = if (localModel.liked) Pref.LIKED else Pref.DISLIKED
            return TrackModel(
                    localModel.name,
                    localModel.trackId,
                    localModel.uri,
                    localModel.previewUrl,
                    null,
                    true,
                    localModel.popularity,
                    pref,
                    localModel.artistName,
                    localModel.coverImageUrl
            )
        }

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
                    apiModel.popularity,
                    Pref.UNSEEN,
                    artists[0].name,
                    images?.get(0)?.url
            )
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(id)
        parcel.writeString(uri)
        parcel.writeString(preview_url)
        parcel.writeParcelable(album, flags)
        parcel.writeValue(is_playable)
        parcel.writeValue(popularity)
        parcel.writeParcelable(pref, flags)
        parcel.writeString(artistName)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

}

data class TrackAlbum(val id: String, val uri: String, val artists: List<Artist>, val images: List<SimpleImage>?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.createTypedArrayList(Artist),
            parcel.createTypedArrayList(SimpleImage)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(uri)
        parcel.writeTypedList(artists)
        parcel.writeTypedList(images)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TrackAlbum> {
        override fun createFromParcel(parcel: Parcel): TrackAlbum {
            return TrackAlbum(parcel)
        }

        override fun newArray(size: Int): Array<TrackAlbum?> {
            return arrayOfNulls(size)
        }
    }
}

data class Artist(val name: String, val id: String, val uri: String) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readString(), parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(id)
        parcel.writeString(uri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Artist> {
        override fun createFromParcel(parcel: Parcel): Artist {
            return Artist(parcel)
        }

        override fun newArray(size: Int): Array<Artist?> {
            return arrayOfNulls(size)
        }
    }
}

