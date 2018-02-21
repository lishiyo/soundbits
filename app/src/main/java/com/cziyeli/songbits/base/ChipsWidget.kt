package com.cziyeli.songbits.base

import android.arch.lifecycle.LifecycleObserver
import android.content.Context
import android.graphics.Color
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviSubView
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.base.ChipsActionProcessor
import com.cziyeli.domain.base.ChipsResultMarker
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
import com.google.android.flexbox.FlexboxLayout
import com.jakewharton.rxrelay2.PublishRelay
import fisk.chipcloud.ChipCloud
import fisk.chipcloud.ChipCloudConfig
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Marker interface for any object that can go in a chip cloud.
 */
interface Chip

/**
 * Events in a [ChipsWidget].
 */
sealed class ChipsIntent : MviIntent {

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
        val root = LayoutInflater.from(context).inflate(R.layout.widget_chips, this, true)

        val config = ChipCloudConfig()
                .selectMode(ChipCloud.SelectMode.multi)
                .checkedChipColor(Color.parseColor("#ddaa00"))
                .checkedTextColor(Color.parseColor("#ffffff"))
                .uncheckedChipColor(Color.parseColor("#e0e0e0"))
                .uncheckedTextColor(Color.parseColor("#000000"))
                .useInsetPadding(true)
                .typeface(ResourcesCompat.getFont(context, R.font.quicksand))

        chipCloud = ChipCloud(context, this, config)
        chipCloud.addChip("HelloWorld!")
//        chipCloud.setChecked(0)

        // listeners => view model
        chipCloud.setListener { index, checked, userClick ->
            if (userClick) {
                Utils.mLog(TAG, String.format("chipCheckedChange Label at index: %d checked: %s", index, checked))
            }
        }

        App.appComponent.inject(this)
    }

    fun addChips(chips: List<Chip>) {
        chipCloud.addChips(chips)
    }

    override fun processIntents(intents: Observable<out ChipsIntent>) {
        compositeDisposable.add(
                intents.subscribe(eventsPublisher::accept)
        )
    }

    override fun states(): Observable<ChipsViewModel.ViewState> {
        return viewModel.states()
    }

    override fun render(state: ChipsViewModel.ViewState) {
        Utils.mLog(TAG, "render!")
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

    override fun processIntents(intents: Observable<out ChipsIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
       return viewStates
    }

    data class ViewState(
            val chips: List<Chip> = listOf()
    ) : MviViewState
}