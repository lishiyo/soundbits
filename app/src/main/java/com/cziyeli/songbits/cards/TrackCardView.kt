package com.cziyeli.songbits.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cziyeli.commons.Utils
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.*
import info.abdolahi.CircularMusicProgressBar
import info.abdolahi.OnCircularSeekBarChangeListener
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

    @View(R.id.track_image_container)
    private lateinit var trackImageContainer: android.view.View

    @View(R.id.track_image)
    private lateinit var trackImage: CircularMusicProgressBar

    @View(R.id.track_play_pause)
    private lateinit var trackPlayPause: ImageView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @View(R.id.track_title_wrapper)
    private lateinit var trackTitleWrapper: android.view.View


    // Flag to flip the icon
    private var isPlaying: Boolean = true

    private val seekBarChangeListener = object : OnCircularSeekBarChangeListener {
        override fun onProgressChanged(circularBar: CircularMusicProgressBar?, progress: Int, fromUser: Boolean) {
            Utils.mLog(TAG, "onProgressChanged", "progress: $progress -- fromUser: $fromUser")
        }

        override fun onClick(circularBar: CircularMusicProgressBar?) {
            circularBar?.performClick()
        }

        override fun onLongPress(circularBar: CircularMusicProgressBar?) {
            // no-op
            Utils.mLog(TAG, "onLongPress")
        }
    }

    private val trackImageClickListener = android.view.View.OnClickListener {
        // pause/resume
        listener.getPlayerIntents()
                .onNext(TrackIntent.CommandPlayer.create(PlayerInterface.Command.PAUSE_OR_RESUME, model))

        // switch icon
        val icon = if (isPlaying) R.drawable.basic_pause else R.drawable.basic_play
        trackPlayPause.setImageResource(icon)
        isPlaying = !isPlaying
    }

    @Resolve
    private fun onResolved() {
        Utils.log(TAG, "onResolved: ${model.name}") // on average will load 3

        trackName.text = model.name
        artistName.text = model.artistName

        trackImage.setOnCircularBarChangeListener(seekBarChangeListener)
        trackImage.setOnClickListener(trackImageClickListener)

        Glide.with(context)
                .load(model.imageUrl)
                .into(trackImage)
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

        trackImageContainer.alpha = 1f
        Utils.setVisible(trackTitleWrapper, true)
        Utils.setVisible(trackPlayPause, true)
    }

    @SwipeOut
    private fun onSwipedOut() {
        Utils.log(TAG, "onSwipedOut (rejected): ${model.name}")

        finishTrack()

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

        finishTrack()

        // add this track to the Liked list
        listener.getTrackIntents().onNext(
                TrackIntent.ChangeTrackPref.like(model)
        )
    }

    private fun finishTrack() {
        // stop playing this track
        listener.getPlayerIntents().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))

        Utils.setVisible(trackTitleWrapper, false)
        trackImage.setOnCircularBarChangeListener(null)
        trackImage.setOnClickListener(null)
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
