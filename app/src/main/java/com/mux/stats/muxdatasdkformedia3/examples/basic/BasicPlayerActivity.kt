package com.mux.stats.muxdatasdkformedia3.examples.basic

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView.KEEP_SCREEN_ON
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding
import com.mux.stats.muxdatasdkformedia3.toMediaItem
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData

class BasicPlayerActivity : AppCompatActivity() {

  private lateinit var view: ActivityPlayerBinding
  private var player: Player? = null
  private var muxStats: MuxStatsSdkMedia3<ExoPlayer>? = null

  @OptIn(UnstableApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityPlayerBinding.inflate(layoutInflater)
    setContentView(view.root)

    view.playerView.apply {
      setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
    }
    window.addFlags(KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    startPlaying(
      intent.getStringExtra(EXTRA_URL) ?: Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY
    )
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
      newPlayer.prepare()
      newPlayer.playWhenReady = true
    }
  }

  private fun stopPlaying() {
    player?.let { oldPlayer ->
      oldPlayer.stop()
      oldPlayer.release()
    }
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
    return ExoPlayer.Builder(this)
      .build().apply {
        addListener(object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            Log.e(javaClass.simpleName, "player error!", error)
            Toast.makeText(this@BasicPlayerActivity, error.localizedMessage, Toast.LENGTH_SHORT)
              .show()
          }

          override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
          ) {

            Log.d("LEARNSEEK", "positionDiscontinuity")
            Log.d("LEARNSEEK", "oldPosition ${oldPosition.strrring()}")
            Log.d("LEARNSEEK", "newPosition ${newPosition.strrring()}")
            Log.d("LEARNSEEK", "reason $reason")
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
          }

        })
      }
  }

  fun PositionInfo.strrring(): String {
    return "PositionInfo:{\n" +
            "\t windowUId: $windowUid\n" +
            "\t mediaItemIndex: $mediaItemIndex\n" +
            "\t contentPositionMs: $contentPositionMs\n" +
            "\t positionMs: $positionMs\n" +
            "\n}"
  }

  companion object {
    const val EXTRA_URL: String = "com.mux.video.url"
  }
}
