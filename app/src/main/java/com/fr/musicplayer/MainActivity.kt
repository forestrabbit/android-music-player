package com.fr.musicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), Notify {
    private var playButtonState = 0
    private var stateButtonState = 0

    private lateinit var playButton: ImageButton
    private lateinit var stateButton: ImageButton
    private lateinit var musicDisplay: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomAdapter
    private lateinit var seekBar: SeekBar
    private lateinit var service: Intent
    private var isInit = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var audioManager: AudioManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        service = Intent(this, MusicService::class.java)
        playButton = findViewById(R.id.play)
        stateButton = findViewById(R.id.state)
        musicDisplay = findViewById(R.id.nowMusicDisplay)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        recyclerView.itemAnimator = DefaultItemAnimator()
        audioManager = AudioManager(applicationContext)
        audioManager.register(this)
        adapter = object : CustomAdapter(audioManager.audioList) {
            override fun onclick(view: View, position: Int) {
                audioManager.position = position
                audioManager.stop()
                if (!isInit) {
                    isInit = true
                    startForegroundService(service)
                } else {
                    audioManager.play()
                }
                playButtonState = 0
                changeButton("play")
            }
        }
        recyclerView.adapter = adapter
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            initMusicList()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        seekBar = findViewById(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    audioManager.changeProgress(p1.toLong())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //NOTHING
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //NOTHING
            }

        })
        playButtonState = if (audioManager.getPauseState()) {
            1
        } else {
            0
        }

        when (audioManager.state) {
            "List" -> {stateButtonState = 0}
            "Loop" -> {stateButtonState = 1}
        }

        changeButton("play")
        changeButton("state")
    }

    override fun setTime(time: Long, total: Long) {
        seekBar.max = total.toInt()
        seekBar.progress = time.toInt()
    }

    private fun changeButton(buttonName: String) {
        when (buttonName) {
            "play" -> {
                when (playButtonState) {
                    0 -> {
                        playButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_pause_24))
                    }
                    1 -> {
                        playButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_play_arrow_24))
                    }
                }
            }
            "state" -> {
                when (stateButtonState) {
                    0 -> {
                        stateButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_playlist_play_24))
                    }
                    1 -> {
                        stateButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_loop_24))
                    }
                }
            }
        }
    }

    fun onclick(v: View) {
        when (v.id) {
            R.id.play -> {
                when (playButtonState) {
                    0 -> {
                        playButtonState = 1
                        audioManager.pause()
                    }
                    1 -> {
                        playButtonState = 0
                        if (isInit) {
                            audioManager.play()
                        } else {
                            startForegroundService(service)
                            isInit = true
                        }
                    }
                }
                changeButton("play")
            }
            R.id.state -> {
                when (stateButtonState) {
                    0 -> {
                        audioManager.state = "Loop"
                        stateButtonState = 1
                    }
                    1 -> {
                        audioManager.state = "List"
                        stateButtonState = 0
                    }
                }
                changeButton("state")
            }
            R.id.left -> {
                if (isInit) {
                    audioManager.position--
                    audioManager.stop()
                    audioManager.play()
                }
            }
            R.id.right -> {
                if (isInit) {
                    audioManager.position++
                    audioManager.stop()
                    audioManager.play()
                }
            }
        }
    }

    private fun initMusicList() {
        audioManager.query()
    }

    override fun musicListPrepared(audioList: MutableList<Audio>) {
        adapter.notifyItemRangeChanged(0, audioList.size)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initMusicList()
                } else {
                    Toast.makeText(this, "需要存储权限来访问音乐文件", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    override fun setHint(str: String) {
        musicDisplay.text = str
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(service)
    }
}