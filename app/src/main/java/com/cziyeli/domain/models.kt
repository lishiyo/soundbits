package com.cziyeli.domain

import android.os.Parcel
import android.os.Parcelable
import kaaes.spotify.webapi.android.models.Image

data class SimpleImage(val height: Int?, val width: Int?, val url: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString()) {
    }

    constructor(image: Image) : this(image.height, image.width, image.url)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(height)
        parcel.writeValue(width)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SimpleImage> {
        override fun createFromParcel(parcel: Parcel): SimpleImage {
            return SimpleImage(parcel)
        }

        override fun newArray(size: Int): Array<SimpleImage?> {
            return arrayOfNulls(size)
        }
    }
}