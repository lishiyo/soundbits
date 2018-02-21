package com.cziyeli.songbits.base

import android.arch.lifecycle.LifecycleObserver
import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.domain.base.*
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.profile.ProfileIntentMarker
import com.google.android.flexbox.AlignContent.FLEX_START
import com.google.android.flexbox.AlignContent.SPACE_AROUND
import com.google.android.flexbox.FlexWrap.WRAP
import com.google.android.flexbox.FlexboxLayout
import com.jakewharton.rxrelay2.PublishRelay
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
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
sealed class ChipsIntent : MviIntent, ProfileIntentMarker {

    // GET https://api.spotify.com/v1/recommendations/available-genre-seeds
    class FetchSeedGenres(val limit: Int = 200) : ChipsIntent()

    // User checked or unchecked a chip
    class SelectionChange(val index: Int, val selected: Boolean) : ChipsIntent()
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
) : FlexboxLayout(context, attrs, defStyleAttr), MviSubView<ChipsIntent, ChipsViewModel.ViewState> {
    private val TAG = ChipsWidget::class.java.simpleName

    // Container for the chips
    private val chipCloud: ChipCloud

    @Inject
    lateinit var viewModel: ChipsViewModel

    private val eventsPublisher = PublishRelay.create<ChipsIntent>()
    private val compositeDisposable = CompositeDisposable()

    init {
        App.appComponent.inject(this)

        LayoutInflater.from(context).inflate(R.layout.widget_chips, this, true)
        initViewModel()

        // set flexbox
        alignContent = SPACE_AROUND
        alignItems = FLEX_START
        flexWrap = WRAP
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        setDividerDrawable(resources.getDrawable(R.drawable.chip_div))

        val config = ChipCloudConfig()
                .selectMode(ChipCloud.SelectMode.multi)
                .checkedChipColor(resources.getColor(R.color.colorAccent))
                .checkedTextColor(resources.getColor(R.color.colorWhite))
                .uncheckedChipColor(resources.getColor(R.color.colorWhite))
                .uncheckedTextColor(resources.getColor(R.color.colorAccent))
                .useInsetPadding(false)
                .typeface(ResourcesCompat.getFont(context, R.font.quicksand))

        chipCloud = ChipCloud(context, this, config)
        chipCloud.addChip("HelloWorld!")

        // listeners => view model
        chipCloud.setListener { index, checked, userClick ->
            if (userClick) {
                Utils.mLog(TAG, String.format("chipCheckedChange Label at index: %d checked: %s", index, checked))
                eventsPublisher.accept(ChipsIntent.SelectionChange(index, checked))
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
    override fun processIntents(intents: Observable<out ChipsIntent>) {
        compositeDisposable.add(
                intents.subscribe(eventsPublisher::accept)
        )
    }

    override fun states(): Observable<ChipsViewModel.ViewState> {
        return viewModel.states()
    }

    override fun render(state: ChipsViewModel.ViewState) {
        Utils.mLog(TAG, "render! $state")
        when {
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ChipsResult.FetchSeedGenres -> {
                chipCloud.addChips(state.chips.map { it.toString() })
            }
            state.status == MviViewState.Status.SUCCESS && state.lastResult is ChipsResult.SelectionChange -> {
                // re-render just that chip
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        compositeDisposable.clear()
    }

}

class ChipsViewModel @Inject constructor(
        val actionProcessor: ChipsActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<ChipsIntent, ChipsViewModel.ViewState, ChipsResultMarker> {
    private val TAG = ChipsViewModel::class.simpleName

    private val viewStates: PublishRelay<ViewState> by lazy { PublishRelay.create<ViewState>() }
    private val intentsSubject : PublishRelay<ChipsIntent> by lazy { PublishRelay.create<ChipsIntent>() }
    private val compositeDisposable = CompositeDisposable()

    private val reducer: BiFunction<ViewState, ChipsResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
            is ChipsResult.FetchSeedGenres -> return@BiFunction processFetchSeedGenres(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        val observable: Observable<ViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<ChipsActionMarker>())
                .compose(actionProcessor.combinedProcessor)
                .compose(resultFilter<ChipsResultMarker>())
                .observeOn(schedulerProvider.ui())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .scan(ViewState(), reducer) // final scan

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.mLog(TAG, "init", "error", err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: ChipsIntent) : MviAction {
        return when (intent) {
            is ChipsIntent.FetchSeedGenres -> ChipsAction.FetchSeedGenres(intent.limit)
            is ChipsIntent.SelectionChange -> ChipsAction.SelectionChange(intent.index, intent.selected)
            else -> None
        }
    }

    override fun processIntents(intents: Observable<out ChipsIntent>) {
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
                        status = status
                )
            }
            MviResult.Status.SUCCESS -> {
                val chips = result.genres.map { Seed(it) }
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = status,
                        chips = chips
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
            val selected: List<Int> = listOf() // currently selected indices
    ) : MviViewState {
        val selectedChips: List<Chip>
            get() = selected.map { chips[it] }

        override fun toString(): String {
            return "total: ${chips.size}, selected: ${selectedChips.size}"
        }
    }
}