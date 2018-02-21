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
                   Observable.empty()
            ).doOnNext {
                Utils.mLog(TAG, "PlaylistCardActionProcessor: --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    private val seedGenresProcessor:  ObservableTransformer<ChipsAction.FetchSeedGenres, ChipsResult.FetchSeedGenres> = ObservableTransformer {
        actions -> actions.switchMap { act ->
            Observable.just(GENRE_SEEDS)
                .map { seeds -> ChipsResult.FetchSeedGenres.createSuccess(seeds) }
                .onErrorReturn { err -> ChipsResult.FetchSeedGenres.createError(err) }
                .observeOn(schedulerProvider.ui())
                .startWith(ChipsResult.FetchSeedGenres.createLoading())
        }
    }

}

// ========== ACTIONS ========

interface ChipsActionMarker : MviAction

sealed class ChipsAction : ChipsActionMarker {
    // GET https://api.spotify.com/v1/recommendations/available-genre-seeds
    class FetchSeedGenres(limit: Int = 200) : ChipsAction()

    // User checked or unchecked a chip
    class SelectionChange(index: Int, selected: Boolean) : ChipsAction()
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

    class SelectionChange(val index: Int, val checked: Boolean) : ChipsResult(MviResult.Status.SUCCESS)
}