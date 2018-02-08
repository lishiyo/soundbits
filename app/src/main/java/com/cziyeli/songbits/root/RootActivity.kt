package com.cziyeli.songbits.root

import android.arch.lifecycle.Observer
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
import com.cziyeli.songbits.home.HomeFragment
import com.cziyeli.songbits.profile.UserFragment
import com.cziyeli.songbits.stash.StashFragment
import com.jakewharton.rxrelay2.PublishRelay
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_root.*
import javax.inject.Inject

// Main activity with bottom nav, the three tabs
class RootActivity : AppCompatActivity(), HasSupportFragmentInjector, MviView<RootIntent, RootViewState> {
    private val TAG = RootActivity::class.java.simpleName

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    // view models
    private lateinit var viewModel: RootViewModel

    // intents
    private val eventsPublisher: PublishRelay<RootIntent> by lazy {
        PublishRelay.create<RootIntent>()
    }

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

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

        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(this, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // Bind ViewModel to merged intents stream
        viewModel.processIntents(intents())
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

}