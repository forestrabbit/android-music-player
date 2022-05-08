package com.fr.musicplayer

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.util.concurrent.TimeUnit

class AudioManager(private var context: Context) {
    private var mediaPlayer = MediaPlayer()
    val audioList = mutableListOf<Audio>()
    private val notifyList = mutableListOf<Notify>()
    private var isInit = false
    private var isPaused = false
    var state = "List"
    var position = 0
        set(value) {
            field = value
            if (field < 0) {
                field = audioList.size - 1
            } else if (field == audioList.size) {
                field = 0
            }
        }

    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE
    )
    private val selectionArgs = arrayOf(
        TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS).toString()
    )
    private fun getCountingThread(): Thread {
        return Thread {
            try {
                while (true) {
                    for (notifyObject in notifyList) {
                        notifyObject.setTime(mediaPlayer.currentPosition.toLong(), audioList[position].duration)
                    }
                    Thread.sleep(1)
                }
            } catch (e: InterruptedException) {
                //pass
            }
        }
    }

    private var t = getCountingThread()

    fun query() {
        Thread {
            val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
            val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
            val query = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            query?.use {cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getInt(sizeColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    audioList += Audio(contentUri, name, duration, size)
                }
            }
            for (notifyObject in notifyList) {
                notifyObject.musicListPrepared(audioList)
            }
        }.start()
    }

    fun register(notifyObject: Notify) {
        notifyList.add(notifyObject)
    }

    fun play() {
        Thread {
            if (!isInit) {
                isInit = true
                isPaused = false
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, audioList[position].uri)
                    prepare()
                }
                mediaPlayer.setOnCompletionListener {
                    t.interrupt()
                    isInit = false
                    isPaused = false
                    when (state) {
                        "List" -> {
                            position++
                            play()
                        }
                        "Loop" -> {
                            play()
                        }
                    }
                }
                mediaPlayer.start()
                for (notifyObject in notifyList) {
                    notifyObject.setHint(audioList[position].name)
                }
                t = getCountingThread()
                t.start()
            } else {
                isPaused = false
                mediaPlayer.start()
            }

        }.start()
    }

    fun stop() {
        isInit = false
        isPaused = false
        if (mediaPlayer.isPlaying) {
            t.interrupt()
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    fun pause() {
        if (isInit) {
            mediaPlayer.pause()
            isPaused = true
        }
    }

    fun changeProgress(nowTime: Long) {
        mediaPlayer.seekTo(nowTime, MediaPlayer.SEEK_CLOSEST_SYNC)
    }

    fun getPauseState(): Boolean {
        return isPaused || !isInit
    }
}