package com.mux.stats.muxdatasdkformedia3.examples.ima

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.ui.PlayerView
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding
import com.mux.stats.muxdatasdkformedia3.toMediaItem
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.media3_ima.monitorWith
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData

class ImaAdsActivity : AppCompatActivity() {

  private lateinit var view: ActivityPlayerBinding
  private var player: Player? = null
  private var muxStats: MuxStatsSdkMedia3<ExoPlayer>? = null
  private var adsLoader: ImaAdsLoader? = null

  @OptIn(UnstableApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityPlayerBinding.inflate(layoutInflater)
    setContentView(view.root)

    view.playerView.apply {
      setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
    }
  }

  override fun onResume() {
    super.onResume()
    startPlaying(Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY)
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  private fun startPlaying(mediaUrl: String) {
    stopPlaying()

    player = createPlayer().also { newPlayer ->
      muxStats = monitorPlayer(newPlayer)
      adsLoader = ImaAdsLoader.Builder(this)
        .monitorWith(
          muxStats = muxStats!!,
          customerAdErrorListener = { /*Optional, your custom logic*/ },
          customerAdEventListener = { /*Optional, your custom logic*/ },
        )
        .build()

      view.playerView.player = newPlayer
      newPlayer.setMediaItem(mediaUrl.toMediaItem())
      newPlayer.prepare()
      newPlayer.playWhenReady = true
    }
  }

  private fun stopPlaying() {
    player?.let { oldPlayer ->
      oldPlayer.stop()
      oldPlayer.release()
    }
    adsLoader?.setPlayer(null)

    // Make sure to release() your muxStats whenever the user is done with the player
    muxStats?.release()
  }

  private fun monitorPlayer(player: ExoPlayer): MuxStatsSdkMedia3<ExoPlayer> {
    // You can add your own data to a View, which will override any data we collect
    val customerData = CustomerData(
      CustomerPlayerData().apply { },
      CustomerVideoData().apply {
        title = "Mux Data SDK for Media3 Demo"
      },
      CustomerViewData().apply { }
    )

    return player.monitorWithMuxData(
      context = this,
      envKey = Constants.MUX_DATA_ENV_KEY,
      customerData = customerData,
      playerView = view.playerView
    )
  }

  private fun createPlayer(): ExoPlayer {
    // TODO: Add the IMA-related media source stuff
    return ExoPlayer.Builder(this)
      .build()
      .apply {
        addListener(object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            Log.e(javaClass.simpleName, "player error!", error)
            Toast.makeText(this@ImaAdsActivity, error.localizedMessage, Toast.LENGTH_SHORT).show()
          }
        })
      }
  }
}