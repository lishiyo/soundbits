package com.cziyeli.domain.user

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.tracks.TrackModel

interface ProfileResultMarker : CardResultMarker

sealed class ProfileResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : ProfileResultMarker {

    class FetchRecommendedTracks(status: MviResult.Status, error: Throwable?, val tracks: List<TrackModel> = listOf()
    ) : ProfileResult(status, error) {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : FetchRecommendedTracks {
                return FetchRecommendedTracks(MviResult.Status.SUCCESS, null, tracks)
            }
            fun createError(throwable: Throwable) : FetchRecommendedTracks {
                return FetchRecommendedTracks(MviResult.Status.ERROR, throwable)
            }
            fun createLoading(): FetchRecommendedTracks {
                return FetchRecommendedTracks(MviResult.Status.LOADING, null)
            }
        }
    }

}