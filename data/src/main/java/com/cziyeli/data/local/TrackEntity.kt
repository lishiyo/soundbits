package com.cziyeli.data.local

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey



/**
 * Created by connieli on 1/1/18.
 */
@Entity(tableName = "com.cziyeli.domain.tracks")
class TrackEntity {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "artist_name")
    var artistName: String? = null

    @ColumnInfo(name = "uri")
    var uri: String? = null

    @ColumnInfo(name = "href")
    var href: String? = null

    @ColumnInfo(name = "preview_url")
    var previewUrl: String? = null

    @ColumnInfo(name = "popularity")
    var popularity: Int = 0

    @ColumnInfo(name = "liked")
    var liked: Boolean = false

}