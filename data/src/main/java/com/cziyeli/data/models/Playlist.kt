package com.cziyeli.data.models

// All the info needed to create a playlist
data class PlaylistData(val ownerId: Int,
                        val name: String,
                        val description: String?, // optional description
                        val public: Boolean = false)

