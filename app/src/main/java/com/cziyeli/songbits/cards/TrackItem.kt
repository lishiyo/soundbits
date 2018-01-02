package com.cziyeli.songbits.cards

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.songbits.R
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.SwipePlaceHolderView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.*

/**
 * Created by connieli on 1/2/18.
 */
@Layout(R.layout.cards_track_view)
class TrackItem(private val context: Context,
                private val model: TrackCard,
                private val swipeView: SwipePlaceHolderView) {

    @View(R.id.image)
    private lateinit var imageView: SimpleDraweeView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @SwipeView
    private val cardView: android.view.View? = null

    @Resolve
    private fun onResolved() {
        trackName.setText(model.name)
        artistName.setText(model.artist?.name)

        imageView.setImageURI(model.coverImage?.url)
    }

    @SwipeHead
    private fun onSwipeHeadCard() {
//        Glide.with(mContext).load(mProfile.getImageUrl())
//                .bitmapTransform(RoundedCornersTransformation(
//                        mContext, Utils.dpToPx(7), 0,
//                        RoundedCornersTransformation.CornerType.TOP))
//                .into(profileImageView)
//        cardView!!.invalidate()
    }

    @Click(R.id.image)
    private fun onClick() {
        Log.d("EVENT", "image click")
        //        mSwipeView.addView(this);
    }

    @SwipeOut
    private fun onSwipedOut() {
        Log.d("EVENT", "onSwipedOut")
        //        mSwipeView.addView(this);
    }

    @SwipeCancelState
    private fun onSwipeCancelState() {
        Log.d("EVENT", "onSwipeCancelState")
    }

    @SwipeIn
    private fun onSwipeIn() {
        Log.d("EVENT", "onSwipedIn")
    }

    @SwipeInState
    private fun onSwipeInState() {
        Log.d("EVENT", "onSwipeInState")
    }

    @SwipeOutState
    private fun onSwipeOutState() {
        Log.d("EVENT", "onSwipeOutState")
    }
}
