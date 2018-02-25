package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.domain.summary.SummaryAction
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserActionProcessor
import com.cziyeli.domain.user.UserResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Business logic to convert actions => results on the [RootActivity] screen.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class RootActionProcessor @Inject constructor(private val repository: Repository,
                                              private val schedulerProvider: BaseSchedulerProvider,
                                              private val userActionProcessor: UserActionProcessor
) {
    private val TAG = RootActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<MviAction, MviResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<MviResult>(
                    shared.ofType<UserAction.FetchQuickCounts>(UserAction.FetchQuickCounts::class.java)
                            .compose(userActionProcessor.fetchQuickCountsProcessor),
                    shared.ofType<UserAction.LoadLikedTracks>(UserAction.LoadLikedTracks::class.java).compose(likedTracksProcessor),
                    shared.ofType<UserAction.LoadDislikedTracks>(UserAction.LoadDislikedTracks::class.java).compose(dislikedTracksProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    val likedTracksProcessor : ObservableTransformer<UserAction.LoadLikedTracks, UserResult.LoadLikedTracks>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.LIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadLikedTracks.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadLikedTracks.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(UserResult.LoadLikedTracks.createLoading())
        }
    }

    val dislikedTracksProcessor : ObservableTransformer<UserAction.LoadDislikedTracks, UserResult.LoadDislikedTracks>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.DISLIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadDislikedTracks.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadDislikedTracks.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(UserResult.LoadDislikedTracks.createLoading())
            }
        }

    /**
     * Stash tracks in database.
     */
     val saveTracksProcessor: ObservableTransformer<SummaryAction.SaveTracks, SummaryResult.SaveTracks> = ObservableTransformer {
        actions -> actions.switchMap({ action ->
            Observable.just(action)
                .doOnNext { repository.saveTracksLocal(mapNewTracks(action)) }
                .map { act -> SummaryResult.SaveTracks.createSuccess(act.tracks, act.playlistId) }
                .onErrorReturn { err -> SummaryResult.SaveTracks.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(SummaryResult.SaveTracks.createLoading())
        })
    }

    private fun mapNewTracks(act: SummaryAction.SaveTracks) : List<TrackEntity> {
        return act.tracks.map {
            TrackEntity(
                    trackId = it.id,
                    name = it.name,
                    uri = it.uri,
                    previewUrl = it.preview_url,
                    liked = it.pref == TrackModel.Pref.LIKED,
                    cleared = false,
                    playlistId = act.playlistId,
                    artistName = it.artistName,
                    popularity = it.popularity,
                    coverImageUrl = it.imageUrl
            )
        }
    }
}