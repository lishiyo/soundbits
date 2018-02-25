package com.cziyeli.domain.base

import com.cziyeli.commons.GENRE_SEEDS
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents all A=action processing for a chips widget.
 *
 * Created by connieli on 2/20/18.
 */
@Singleton
class ChipsActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = ChipsActionProcessor::class.simpleName
    val combinedProcessor: ObservableTransformer<ChipsActionMarker, ChipsResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<ChipsResultMarker>(
                    // given tracks list -> grab stats
                    shared.ofType<ChipsAction.FetchSeedGenres>(ChipsAction.FetchSeedGenres::class.java)
                            .compose(seedGenresProcessor),
                    shared.ofType<ChipsAction.SelectionChange>(ChipsAction.SelectionChange::class.java).compose(changeSelectionProcessor),
                    shared.ofType<ChipsAction.PickRandomGenres>(ChipsAction.PickRandomGenres::class.java)
                            .compose(pickRandomGenresProcessor),
                    shared.ofType<ChipsAction.Reset>(ChipsAction.Reset::class.java).compose(clearSelectionsProcessor)
            ).doOnNext {
                Utils.mLog(TAG, "ChipsActionProcessor: --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    private val seedGenresProcessor: ObservableTransformer<ChipsAction.FetchSeedGenres, ChipsResult.FetchSeedGenres> = ObservableTransformer {
        actions -> actions.switchMap { act ->
            Observable.just(GENRE_SEEDS)
                    .subscribeOn(schedulerProvider.io())
                    .map { seeds -> ChipsResult.FetchSeedGenres.createSuccess(seeds) }
                    .onErrorReturn { err -> ChipsResult.FetchSeedGenres.createError(err) }
                    .startWith(ChipsResult.FetchSeedGenres.createLoading())
        }
    }

    private val changeSelectionProcessor: ObservableTransformer<ChipsAction.SelectionChange, ChipsResult.ChangeSelection> =
            ObservableTransformer {
        actions -> actions.map { act -> ChipsResult.ChangeSelection(act.index, act.selected) }
                    .subscribeOn(schedulerProvider.io())
    }

    private val clearSelectionsProcessor: ObservableTransformer<ChipsAction.Reset, ChipsResult.ChangeSelections> =
            ObservableTransformer {
                actions -> actions.map { act -> ChipsResult.ChangeSelections(listOf()) }
                    .subscribeOn(schedulerProvider.io())
            }

    private val pickRandomGenresProcessor: ObservableTransformer<ChipsAction.PickRandomGenres, ChipsResult.ChangeSelections> =
            ObservableTransformer {
                actions -> actions.map { act -> ChipsResult.ChangeSelections(Utils.getRandomGenreSeeds(), true) }
                    .subscribeOn(schedulerProvider.io())
            }
}

// ========== ACTIONS ========

interface ChipsActionMarker : MviAction

sealed class ChipsAction : ChipsActionMarker {
    // GET https://api.spotify.com/v1/recommendations/available-genre-seeds
    class FetchSeedGenres(val limit: Int = 200) : ChipsAction()

    // User checked or unchecked a chip
    class SelectionChange(val index: Int, val selected: Boolean) : ChipsAction()

    // User hit 'pick random' genre seeds
    class PickRandomGenres(val count: Int) : ChipsAction()

    // Clear out chips
    class Reset : ChipsAction()
}

// ========== RESULTS ========

interface ChipsResultMarker : MviResult

sealed class ChipsResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null) : ChipsResultMarker {

    class FetchSeedGenres(status: MviResult.Status,
                          error: Throwable?,
                          val genres: List<String> = listOf()
    ) : ChipsResult(status, error) {

        companion object {
            fun createSuccess(genres: List<String>) : FetchSeedGenres {
                return FetchSeedGenres(MviResult.Status.SUCCESS, null, genres)
            }
            fun createError(throwable: Throwable) : FetchSeedGenres {
                return FetchSeedGenres(MviResult.Status.ERROR, throwable)
            }
            fun createLoading(): FetchSeedGenres {
                return FetchSeedGenres(MviResult.Status.LOADING, null)
            }
        }
    }

    class ChangeSelection(val index: Int, val selected: Boolean) : ChipsResult(MviResult.Status.SUCCESS)

    class ChangeSelections(val indicies: List<Int>, val selected: Boolean = true) : ChipsResult(MviResult.Status.SUCCESS)
}