package com.cziyeli.songbits.profile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cziyeli.songbits.R


class UserFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    companion object {
        fun create(args: Bundle? = Bundle()) : UserFragment {
            val fragment = UserFragment()
            fragment.arguments = args
            return fragment
        }
    }
}