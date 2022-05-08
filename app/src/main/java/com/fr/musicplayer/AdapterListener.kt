package com.fr.musicplayer

import android.view.View

interface AdapterListener {
    fun onclick(view: View, position: Int)
}