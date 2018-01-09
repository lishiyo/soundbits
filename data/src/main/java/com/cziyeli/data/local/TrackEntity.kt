package com.cziyeli.data.local

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Represents a [TrackModel] in the database.
 *
 * Created by connieli on 1/1/18.
 */
@Entity(tableName = "Track")
class TrackEntity {

    @PrimaryKey(autoGenerate = true)
    val uid: Long = -1 // id inside database

    @ColumnInfo(name = "track_id", index = true)
    var trackId: Int = 0 // actual track id

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "artist_name")
    var artistName: String? = null

    @ColumnInfo(name = "uri")
    var uri: String? = null

    @ColumnInfo(name = "preview_url")
    var previewUrl: String? = null

    @ColumnInfo(name = "popularity")
    var popularity: Int = 0

    @ColumnInfo(name = "liked") // main pref
    var liked: Boolean = false

    @ColumnInfo(name = "cleared")
    val cleared: Boolean = false // if cleared, don't show to user anymore

}