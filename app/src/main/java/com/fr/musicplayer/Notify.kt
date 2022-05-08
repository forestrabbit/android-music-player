package com.fr.musicplayer

interface Notify {
    fun musicListPrepared(audioList: MutableList<Audio>)
    fun setTime(time: Long, total: Long)
    fun setHint(str: String)
}