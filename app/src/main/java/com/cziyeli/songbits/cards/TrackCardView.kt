package com.cziyeli.songbits.cards

import android.content.Context
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.*
import io.reactivex.subjects.PublishSubject

/**
 * Displays a "card" view of a track for swiping.
 *
 * Created by connieli on 1/2/18.
 */
@Layout(R.layout.cards_track_view)
class TrackCardView(private val context: Context,
                    private val model: TrackModel,
                    private val listener: TrackListener) {

    private val TAG = TrackCardView::class.simpleName

    @View(R.id.image)
    private lateinit var imageView: SimpleDraweeView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @Resolve
    private fun onResolved() {
        trackName.text = model.name
        artistName.text = model.artistName
        imageView.setImageURI(model.imageUrl)

        Utils.log(TAG, "onResolved: ${model.name}") // on average will load 3
    }

    @SwipeHead
    private fun onSwipeHeadCard() {
        // a card comes on top of the stack (follows onResolved)
        Utils.log(TAG, "onSwipeHeadCard: ${model.name} -- clearing pref")

        // immediately start playing
        listener.getPlayerIntents().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.PLAY_NEW, model))

        // 'undo' clears liked/disliked pref
        listener.getTrackIntents().onNext(
                TrackIntent.ChangeTrackPref.clear(model)
        )
    }

    @Click(R.id.image)
    private fun onClick() {
        // pause/resume
        listener.getPlayerIntents().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.PAUSE_OR_RESUME, model))
    }

    @SwipeOut
    private fun onSwipedOut() {
        Utils.log(TAG, "onSwipedOut (rejected): ${model.name}")

        // stop playing this track
        listener.getPlayerIntents().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))

        // add this track to the Pass list
        listener.getTrackIntents().onNext(
                TrackIntent.ChangeTrackPref.dislike(model)
        )
    }

    @SwipeCancelState
    private fun onSwipeCancelState() {
        Utils.log(TAG, "onSwipeCancelState")
    }

    @SwipeIn
    private fun onSwipeIn() {
        Utils.log(TAG, "onSwipeIn (accepted): ${model.name}")

        listener.getPlayerIntents().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))

        // add this track to the Liked list
        listener.getTrackIntents().onNext(
                TrackIntent.ChangeTrackPref.like(model)
        )
    }

    @SwipeInState
    private fun onSwipeInState() {
//        Utils.log(TAG, "onSwipeInState")
    }

    @SwipeOutState
    private fun onSwipeOutState() {
//        Utils.log(TAG, "onSwipeOutState")
    }

    interface TrackListener {
        // emit events to the audio player
        fun getPlayerIntents(): PublishSubject<TrackIntent.CommandPlayer>

        // emit like/dislike events
        fun getTrackIntents(): PublishSubject<TrackIntent.ChangeTrackPref>
    }
}
