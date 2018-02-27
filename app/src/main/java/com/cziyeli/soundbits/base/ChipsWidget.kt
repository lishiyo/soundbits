package com.cziyeli.soundbits.base

import android.arch.lifecycle.LifecycleObserver
import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.domain.base.*
import com.cziyeli.soundbits.R
import com.cziyeli.soundbits.di.App
import com.cziyeli.soundbits.profile.ProfileIntent
import com.cziyeli.soundbits.profile.ProfileIntentMarker
import com.google.android.flexbox.FlexboxLayout
import com.jakewharton.rxrelay2.PublishRelay
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.widget_expandable_chips.view.*
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Marker interface for any object that can go in a chip cloud.
 */
interface Chip

data class Seed(val label: String) : Chip {
    override fun toString(): String {
        return label
    }
}

/**
 * Events in a [ChipsWidget].
 */
interface ChipsIntentMarker : MviIntent, ProfileIntentMarker
sealed class ChipsIntent : ChipsIntentMarker {

    // GET https://api.spotify.com/v1/recommendations/available-genre-seeds
    class FetchSeedGenres(val limit: Int = 200) : ChipsIntent(), SingleEventIntent

    // User checked or unchecked a chip
    class SelectionChange(val index: Int, val selected: Boolean) : ChipsIntent()

    // User hit 'pick random' genre seeds
    class PickRandomGenres(val count: Int = 5) : ChipsIntent()
}

/**
 * A standard [FlexboxLayout] containing [ChipCloud]s.
 * Used for multiselecting tags/pills etc.
 *
 * Created by connieli on 2/20/18.
 */
 class ChipsWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), MviSubView<ChipsIntentMarker, ChipsViewModel.ViewState> {
    private val TAG = ChipsWidget::class.java.simpleName

    // Container for the chips
    private val chipCloud: ChipCloud

    @Inject
    lateinit var viewModel: ChipsViewModel

    private val eventsPublisher = PublishRelay.create<ChipsIntentMarker>()
    private val compositeDisposable = CompositeDisposable()

    init {
        App.appComponent.inject(this)

        LayoutInflater.from(context).inflate(R.layout.widget_expandable_chips, this, true)
        orientation = VERTICAL
        initViewModel()

        val config = ChipCloudConfig()
                .selectMode(ChipCloud.SelectMode.multi)
                .checkedChipColor(resources.getColor(R.color.colorPrimaryShade))
                .checkedTextColor(resources.getColor(R.color.colorWhite))
                .uncheckedChipColor(resources.getColor(R.color.colorWhite))
                .uncheckedTextColor(resources.getColor(R.color.colorAccent))
                .useInsetPadding(false)
                .typeface(ResourcesCompat.getFont(context, R.font.quicksand))

        chipCloud = ChipCloud(context, chips_layout, config)
        chipCloud.setListener { index, checked, userClick ->
            if (userClick) {
                eventsPublisher.accept(ChipsIntent.SelectionChange(index, checked))
            }
        }
    }

    fun getCurrentSelected() : List<String> {
        return viewModel.currentViewState.selectedChips.map { it.toString() }
    }

    fun showOrHideGenres(show: Boolean = true) {
        when {
            show && !chips_expansion_layout.isExpanded -> {
                chips_expansion_layout.expand(true)
            }
            !show && chips_expansion_layout.isExpanded -> {
                chips_expansion_layout.collapse(true)
            }
        }
    }

    private fun initViewModel() {
        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {

                        this.render(state)
                    }
                })
        )

        viewModel.processIntents(eventsPublisher)
    }

    // push down events from parent activity
    override fun processIntents(intents: Observable<out ChipsIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(eventsPublisher::accept)
        )
    }

    override fun render(state: ChipsViewModel.ViewState) {
        Utils.mLog(TAG, "render! $state")
        when {
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ChipsResult.FetchSeedGenres -> {
                chipCloud.addChips(state.chips.map { it.toString() })
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ChipsResult.ChangeSelection -> {
                // see if we need to deselect any
                if (state.deselected.isNotEmpty()) {
                    state.deselected.forEach { chipCloud.deselectIndex(it) }
                }
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ChipsResult.ChangeSelections -> {
                // deselect all of the current
                if (state.deselected.isNotEmpty()) {
                    state.deselected.forEach { chipCloud.deselectIndex(it) }
                }
                chipCloud.setSelectedIndexes(state.selected.toIntArray())
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Utils.mLog(TAG, "detached!")
        compositeDisposable.clear()
    }

}

class ChipsViewModel @Inject constructor(
        val actionProcessor: ChipsActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<ChipsIntentMarker, ChipsViewModel.ViewState, ChipsResultMarker> {
    private val TAG = ChipsViewModel::class.simpleName

    private val viewStates: PublishRelay<ViewState> by lazy { PublishRelay.create<ViewState>() }
    var currentViewState: ChipsViewModel.ViewState
    private val intentsSubject : PublishRelay<ChipsIntentMarker> by lazy { PublishRelay.create<ChipsIntentMarker>() }
    private val compositeDisposable = CompositeDisposable()

    private val intentFilter: ObservableTransformer<ChipsIntentMarker, ChipsIntentMarker> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<ChipsIntentMarker>(
                    shared.ofType(ChipsIntent.FetchSeedGenres::class.java).take(1), // only take one time
                    shared.filter({ intent -> intent !is SingleEventIntent })
            )
        }
    }
    private val reducer: BiFunction<ViewState, ChipsResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
            is ChipsResult.FetchSeedGenres -> return@BiFunction processFetchSeedGenres(previousState, result)
            is ChipsResult.ChangeSelection -> return@BiFunction processSelectionChange(previousState, result)
            is ChipsResult.ChangeSelections -> return@BiFunction processSelectionsChange(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        currentViewState = ViewState()
        val observable: Observable<ViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<ChipsActionMarker>())
                .compose(actionProcessor.combinedProcessor)
                .compose(resultFilter<ChipsResultMarker>())
                .observeOn(schedulerProvider.ui())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .scan(currentViewState, reducer) // final scan

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    currentViewState = viewState
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.mLog(TAG, "init", "error", err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: ChipsIntentMarker) : MviAction {
        return when (intent) {
            is ChipsIntent.FetchSeedGenres -> ChipsAction.FetchSeedGenres(intent.limit)
            is ChipsIntent.SelectionChange -> ChipsAction.SelectionChange(intent.index, intent.selected)
            is ChipsIntent.PickRandomGenres -> ChipsAction.PickRandomGenres(intent.count)
            is ProfileIntent.Reset -> ChipsAction.Reset()
            else -> None
        }
    }

    override fun processIntents(intents: Observable<out ChipsIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
       return viewStates
    }

    // =========== Individual reducers ===========

    private fun processFetchSeedGenres(
            previousState: ViewState,
            result: ChipsResult.FetchSeedGenres
    ) : ViewState {
        val status = Utils.statusFromResult(result.status)
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        deselected = listOf()
                )
            }
            MviResult.Status.SUCCESS -> {
                val chips = result.genres.map { Seed(it) }
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        chips = chips,
                        deselected = listOf()
                )
            }
            else -> previousState
        }
    }

    private fun processSelectionChange(
            previousState: ViewState,
            result: ChipsResult.ChangeSelection
    ) : ViewState {
        val status = Utils.statusFromResult(result.status)
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        deselected = listOf()
                )
            }
            MviResult.Status.SUCCESS -> {
                val newSelected = previousState.selected.toMutableList()
                if (result.selected) {
                    newSelected.add(result.index)
                } else {
                    // remove from the list
                    newSelected.remove(result.index)
                }
                val finalFive = newSelected.takeLast(5)
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        selected = finalFive,
                        deselected = previousState.selected - finalFive
                )
            }
            else -> previousState
        }
    }

    private fun processSelectionsChange(
            previousState: ViewState,
            result: ChipsResult.ChangeSelections
    ) : ViewState {
        val status = Utils.statusFromResult(result.status)
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        deselected = listOf()
                )
            }
            MviResult.Status.SUCCESS -> {
                val newSelected = result.indicies
                val finalFive = newSelected.takeLast(5)
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        selected = finalFive,
                        deselected = previousState.selected - finalFive
                )
            }
            else -> previousState
        }
    }

    data class ViewState(
            val status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
            val error: Throwable? = null,
            val lastResult: ChipsResultMarker? = null,
            val chips: List<Chip> = listOf(),
            val selected: List<Int> = listOf(), // currently selected indices
            val deselected: List<Int> = listOf() // in case we had to deselect one
    ) : MviViewState {
        val selectedChips: List<Chip>
            get() = selected.map { chips[it] }

        override fun toString(): String {
            return "total: ${chips.size}, selected: $selectedChips"
        }
    }
}