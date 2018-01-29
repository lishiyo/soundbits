package com.cziyeli.songbits.root

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.cziyeli.songbits.base.SmartFragmentStatePagerAdapter


class BottomNavAdapter(fragmentManager: FragmentManager) : SmartFragmentStatePagerAdapter(fragmentManager) {
    private val fragments : MutableList<Fragment> = mutableListOf()

    // Our custom method that populates this Adapter with Fragments
    fun addFragments(fragment: Fragment) {
        fragments.add(fragment)
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}