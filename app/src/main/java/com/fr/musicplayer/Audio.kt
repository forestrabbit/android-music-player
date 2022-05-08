package com.fr.musicplayer

import android.net.Uri

data class Audio(val uri: Uri,
                 val name: String,
                 val duration: Long,
                 val size: Int
)