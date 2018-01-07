package com.cziyeli.songbits.cards.summary

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewStub
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.TrackViewState
import com.wang.avi.AVLoadingIndicatorView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_cards.*

/**
 * Created by connieli on 1/6/18.
 */
class SummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dagger
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_summary)


    }

    private fun showSummary(state: TrackViewState) {
        // if we're at the end, hide cards and show summary
        Utils.setVisible(swipeView, false)

        val stub = findViewById<ViewStub>(R.id.summary_stub)
        val summaryContainer = stub.inflate()

        Utils.setVisible(stub, true)

        // bind summary
        val title = "Liked ${state.currentLikes.size} and disliked ${state.currentDislikes.size} " +
                "out of ${state.allTracks.size} tracks!"
        val titleView = summaryContainer.findViewById<TextView>(R.id.title)
        titleView.text = title
        val progressView = summaryContainer.findViewById<AVLoadingIndicatorView>(R.id.progress)
        progressView.smoothToShow()
        // TODO: get the tracksdata
        // when that comes back, hide the progressView and show the track summary
    }

}