package com.cziyeli.commons

import android.content.Context
import android.widget.Toast

/**
 * Created by connieli on 1/4/18.
 */
fun Any.toast(context: Context) {
    Toast.makeText(context, this.toString(), Toast.LENGTH_SHORT).show()
}