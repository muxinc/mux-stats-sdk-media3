package com.mux.stats.muxdatasdkformedia3

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData

class PlayerActivity : AppCompatActivity() {

  companion object {
    const val MUX_DATA_ENV_KEY = "rhhn9fph0nog346n4tqb6bqda" // TODO: YOUR KEY HERE
    const val VOD_TEST_URL_STEVE = "http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8"
    const val VOD_TEST_URL_DRAGON_WARRIOR_LADY =
      "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
  }

  private lateinit var view: ActivityPlayerBinding
  private var player: Player? = null
  private var muxStats: MuxStatsSdkMedia3? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityPlayerBinding.inflate(layoutInflater)
    setContentView(view.root)
  }

  override fun onResume() {
    super.onResume()
    startPlaying(VOD_TEST_URL_DRAGON_WARRIOR_LADY)
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  private fun startPlaying(mediaUrl: String) {
    stopPlaying()

    player = createPlayer().also { newPlayer ->
      muxStats = monitorPlayer(newPlayer)
      view.playerView.player = newPlayer
      newPlayer.setMediaItem(mediaUrl.toMediaItem())
      newPlayer.playWhenReady = true
    }
  }

  private fun stopPlaying() {
    player?.let { oldPlayer ->
      oldPlayer.stop()
      oldPlayer.release()
    }
    muxStats?.release()
  }

  private fun monitorPlayer(player: Player): MuxStatsSdkMedia3 {
    val customerData = CustomerData(
      CustomerPlayerData().apply { },
      CustomerVideoData().apply {
        title = "Mux Data SDK for Media3 Demo"
      },
      CustomerViewData().apply { }
    )

    return player.monitorWithMuxData(
      context = this,
      envKey = MUX_DATA_ENV_KEY,
      customerData = customerData,
      playerView = view.playerView
    )
  }

  private fun createPlayer(): Player {
    return ExoPlayer.Builder(this)
      .build().apply {
        addListener(object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            Log.e(javaClass.simpleName, "player error!", error)
            Toast.makeText(this@PlayerActivity, error.localizedMessage, Toast.LENGTH_SHORT).show()
          }
        })
      }
  }
}
