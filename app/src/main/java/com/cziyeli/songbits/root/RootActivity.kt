package com.cziyeli.songbits.root

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.cziyeli.commons.Utils
import com.cziyeli.commons.fetchColor
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TracksRecyclerViewDelegate
import com.cziyeli.songbits.home.HomeFragment
import com.cziyeli.songbits.profile.UserFragment
import com.cziyeli.songbits.stash.StashFragment
import com.jakewharton.rxrelay2.PublishRelay
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.widget_simple_card.*
import javax.inject.Inject

// Main activity with bottom nav, the three tabs
class RootActivity : AppCompatActivity(), HasSupportFragmentInjector, MviView<RootIntent, RootViewState> {
    private val TAG = RootActivity::class.java.simpleName

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    // view models
    private lateinit var viewModel: RootViewModel
    @Inject
    lateinit var eventsPublisher: PublishRelay<RootIntent>
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    // Views
    private lateinit var pagerAdapter: BottomNavAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        // bind the view model before events
        initViewModel()

        setUpBottomNav()
        setUpViewPager()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(RootViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModel.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())
    }

    /**
     * Exposed such that others can dispatch to the global stream.
     */
    fun getRootPublisher(): PublishRelay<RootIntent> {
        return eventsPublisher
    }

    /**
     * Exposed such that others can subscribe to the 'global' view state.
     */
    fun getStates() : Observable<RootViewState> {
        return viewModel.states()
    }

    override fun intents(): Observable<out RootIntent> {
        return eventsPublisher
    }

    override fun render(state: RootViewState) {
        Utils.mLog(TAG, "RENDER", "$state")
        // render subviews?

    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }


    private fun setUpViewPager() {
        pagerAdapter = BottomNavAdapter(supportFragmentManager)

        // add the tabs
        val tabOne = HomeFragment.create()
        val tabTwo = StashFragment.create()
        val tabThree = UserFragment.create()
        pagerAdapter.addFragments(tabOne)
        pagerAdapter.addFragments(tabTwo)
        pagerAdapter.addFragments(tabThree)

        view_pager.adapter = pagerAdapter
    }

    private fun setUpBottomNav() {
        val home = AHBottomNavigationItem(resources.getString(R.string.tab_title_home), R.drawable.icon_levels_1)
        val tracks = AHBottomNavigationItem(resources.getString(R.string.tab_title_stash), R.drawable.notes_hearts_line)
        val profile = AHBottomNavigationItem(resources.getString(R.string.tab_title_profile), R.drawable.icon_headphones)
        bottom_navigation.addItem(home)
        bottom_navigation.addItem(tracks)
        bottom_navigation.addItem(profile)
        bottom_navigation.currentItem = 0 // default to home
//        bottom_navigation.background = getDrawable(R.drawable.gradient_reds)
        bottom_navigation.defaultBackgroundColor = fetchColor(R.color.colorPrimary)
        bottom_navigation.accentColor = fetchColor(R.color.colorWhite)
        bottom_navigation.inactiveColor = fetchColor(R.color.colorGrey)
        bottom_navigation.titleState = AHBottomNavigation.TitleState.SHOW_WHEN_ACTIVE
        bottom_navigation.isTranslucentNavigationEnabled = true

        bottom_navigation.isBehaviorTranslationEnabled = false
        bottom_navigation.isForceTint = true

        bottom_navigation.setOnTabSelectedListener({ position, wasSelected ->
            if (!wasSelected) {
                view_pager.currentItem = position
            }
            true
        })
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

}