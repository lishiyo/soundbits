package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.domain.user.UserManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistCardActionProcessor @Inject constructor(private val repository: Repository,
                                                      private val userManager: UserManager,
                                                      private val schedulerProvider: BaseSchedulerProvider,
                                                      private val trackActionProcessor: TrackActionProcessor
) {
    private val TAG = PlaylistCardActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<CardActionMarker, CardResultMarker> = ObservableTransformer { acts
        ->
        acts.publish { shared ->
            Observable.merge<CardResultMarker>(
                    // ==== FETCH ACTIONS ====
                    // given any tracks list -> calculate quick count
                    shared.ofType<CardAction.CalculateQuickCounts>(CardAction.CalculateQuickCounts::class.java)
                            .compose(calculateQuickCountsProcessor),
                    // given playlist id -> grab all tracks (from remote) and calculate their stats
                    shared.ofType<StatsAction.FetchAllTracksWithStats>(StatsAction.FetchAllTracksWithStats::class.java)
                            .compose(fetchAllTracksWithStatsProcessor),
                    // given tracks list -> grab stats
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java)
                            .compose(fetchStatsProcessor),
                    // fetch playlist tracks either from database (if swiped only) or from remote (all)
                    shared.ofType<PlaylistCardAction.FetchPlaylistTracks>(PlaylistCardAction.FetchPlaylistTracks::class.java)
                            .filter { it.playlistId != null }
                            .groupBy { it.onlySwiped }
                            .flatMap { grouped ->
                                val compose: Observable<CardResultMarker> = if (grouped.key == true) {
                                    Utils.mLog(TAG, "FetchPlaylistTracks!", "onlySwiped -- fetching stashed")
                                    grouped.compose(fetchStashedTracksIntermediary).compose(mapStashedTracksProcessor)
                                } else {
                                    Utils.mLog(TAG, "FetchPlaylistTracks!", "NOT onlySwiped -- fetching remote")
                                    grouped.compose(fetchAllTracksProcessor)
                                }
                                compose
                            }
            ).mergeWith(
                    // ==== USER INTERACTIONS ====
                    // update a track's pref in the db
                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java)
                            .compose(changePrefAndSaveProcessor)
            ).mergeWith(
                    // ==== USER INTERACTIONS ====
                    // command the player
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java)
                            .compose(trackActionProcessor.commandPlayerProcessor)
            ).doOnNext {
                Utils.log(TAG, "PlaylistCardActionProcessor: --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    // ============= Action -> Result processors ===========

    // fetch quick stats - query in local database for counts
    private val calculateQuickCountsProcessor: ObservableTransformer<CardAction.CalculateQuickCounts, CardResult.CalculateQuickCounts> =
            ObservableTransformer { actions -> actions
                    .map { act -> act.tracks }
                    .compose(calculateQuickCountsIntermediary)
            }

    // fetch all tracks - hits remote
    private val fetchAllTracksProcessor: ObservableTransformer<PlaylistCardAction.FetchPlaylistTracks, PlaylistCardResult.FetchPlaylistTracks> =
            ObservableTransformer { actions -> actions.switchMap { act ->
                repository
                        .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId!!, act.fields, act.limit, act.offset)
                        .filter { resp -> resp.total > 0 }
                        .map { resp -> resp.items.map { it.track } }
                        .map { tracks -> tracks.map { TrackModel.create(it) } }
                        .map { trackCards -> PlaylistCardResult.FetchPlaylistTracks.createSuccess(trackCards, fromLocal = false) }
                        .onErrorReturn { err -> PlaylistCardResult.FetchPlaylistTracks.createError(err, false) }
                        .subscribeOn(schedulerProvider.io())
                        .observeOn(schedulerProvider.ui())
                        .startWith(PlaylistCardResult.FetchPlaylistTracks.createLoading(false))
            }
    }

    // playlist id => fetch all tracks and calculate full stats
    private val fetchAllTracksWithStatsProcessor: ObservableTransformer<StatsAction.FetchAllTracksWithStats,
            StatsResult.FetchAllTracksWithStats> = ObservableTransformer { actions ->
        actions.switchMap { act ->
            repository.fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                    .map { resp -> resp.items.map { it.track } }
                    .map { tracks -> tracks.map { TrackModel.create(it) } }
                    .flatMap { tracks ->
                        repository
                                .fetchTracksStats(Repository.Source.REMOTE, tracks.map { it.id })
                                .subscribeOn(schedulerProvider.io())
                                .map { resp -> Pair(tracks, TrackListStats.create(resp)) }
                    }.map { pair ->
                        StatsResult.FetchAllTracksWithStats.createSuccess(pair.first, pair.second)
                    }
                    .onErrorReturn { err -> StatsResult.FetchAllTracksWithStats.createError(err) }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(StatsResult.FetchAllTracksWithStats.createLoading())
        }
    }

    // given list of tracks -> fetch track stats
    private val fetchStatsProcessor: ObservableTransformer<StatsAction.FetchStats, StatsResult.FetchStats> = ObservableTransformer {
        actions -> actions.switchMap { action ->
        repository.fetchTracksStats(Repository.Source.REMOTE, action.trackIds)
                .map { resp -> TrackListStats.create(resp) }
                .map { trackStats -> StatsResult.FetchStats.createSuccess(trackStats) }
                .onErrorReturn { err -> StatsResult.FetchStats.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(StatsResult.FetchStats.createLoading())
        }
    }

    // like, dislike, undo track AND save in db
    val changePrefAndSaveProcessor : ObservableTransformer<TrackAction.ChangeTrackPref, TrackResult.ChangePrefResult> =
            ObservableTransformer { actions ->
                actions.switchMap { action ->
                    Observable.just(action)
                            .map { act -> act.track.copy(pref = act.pref) }
                            .observeOn(schedulerProvider.io())
                            .doOnNext { track ->
                                // upsert - attempt update first, if that fails then insert
                                val dbModel = mapTrack(track)
                                val foundId = repository.updateTrackPref(dbModel)
                                Utils.mLog(TAG, "updated trackPref! ${dbModel.name} -- $foundId")
                                if (foundId <= 0) {
                                    repository.saveTrackLocal(dbModel)
                                }
                            }
                            .map { TrackResult.ChangePrefResult.createSuccess(it, it.pref) }
                            .onErrorReturn { err -> TrackResult.ChangePrefResult.createError(err) }
                            .subscribeOn(schedulerProvider.io())
                            .startWith(TrackResult.ChangePrefResult.createLoading(action.track))
                }
            }

    // ============= Intermediate processors ===========

    // given playlist id => grab all its track ids from remote => return list of its track entities in db
    private val fetchStashedTracksIntermediary: ObservableTransformer<PlaylistCardAction.FetchPlaylistTracks,
            Pair<PlaylistCardAction, List<TrackEntity>>> =
            ObservableTransformer {
                actions -> actions.switchMap { act ->
                    repository.fetchPlaylistTracks(ownerId = act.ownerId, playlistId = act.playlistId!!)
                            .map { resp -> resp.items.map { it.track } }
                            .switchMap { list ->
                                repository.fetchStashedTracksByIds(trackIds = list.map { it.id }).toObservable()
                            }
                            .subscribeOn(schedulerProvider.io())
                            .map { Pair(act, it) }
                            .doOnNext { Utils.mLog(TAG, "fetchStashedTracksForPlaylist! count: ${it.second.size} ")}
                }
            }

    // given list of TrackEntities=> return Result with domain TrackModels
    private val mapStashedTracksProcessor: ObservableTransformer<
            Pair<PlaylistCardAction, List<TrackEntity>>, PlaylistCardResult.FetchPlaylistTracks> = ObservableTransformer {
                actions -> actions.map { actionListPair ->
                Pair(actionListPair.first, actionListPair.second.map { TrackModel.createFromLocal(it) }) }
                        .map { pair ->
                            PlaylistCardResult.FetchPlaylistTracks.createSuccess(pair.second, fromLocal = true) }
                        .onErrorReturn { err ->
                            PlaylistCardResult.FetchPlaylistTracks.createError(err, true) }
                        .observeOn(schedulerProvider.ui())
                        .startWith(PlaylistCardResult.FetchPlaylistTracks.createLoading(true))
            }

    // given list of track ids -> return Result with quickCounts
    private val calculateQuickCountsIntermediary: ObservableTransformer<List<TrackModel>, CardResult.CalculateQuickCounts> =
            ObservableTransformer { actions -> actions.switchMap { action ->
                        val likedCount = action.count { it.liked }
                        val dislikedCount = action.count { it.disliked }
                Observable.just(Pair(likedCount, dislikedCount))
                                .map { pair -> CardResult.CalculateQuickCounts.createSuccess(pair.first, pair.second) }
                                .onErrorReturn { err -> CardResult.CalculateQuickCounts.createError(err) }
                                .observeOn(schedulerProvider.ui())
                                .startWith(CardResult.CalculateQuickCounts.createLoading())
                }
            }


    private fun mapTrack(it: TrackModel) : TrackEntity {
        return TrackEntity(
                    trackId = it.id,
                    name = it.name,
                    uri = it.uri,
                    previewUrl = it.preview_url,
                    liked = it.pref == TrackModel.Pref.LIKED,
                    cleared = false,
                    artistName = it.artistName,
                    popularity = it.popularity,
                    coverImageUrl = it.imageUrl
            )
    }
}