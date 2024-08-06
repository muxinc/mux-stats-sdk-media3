package com.mux.stats.muxdatasdkformedia3.examples.basic

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.mux.stats.sdk.muxstats.MuxDataSdk
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

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

      lifecycleScope.launch(Dispatchers.Main) {
        val useEnable = true
        val stopBeforeThirdVideo = false

        delay(10_000)
        Log.d("ENABLEDISABLE", "disabling")
        if (useEnable) {
          muxStats?.disable()
        }

        if (!useEnable) {
          Log.d("ENABLEDISABLE", "calling videoChange() 1")
          muxStats?.videoChange(CustomerVideoData().apply {
            videoTitle = "Steve (Second)"
          })
        }
        Log.d("ENABLEDISABLE", "playing without monitoring (or 1st video change)")
        newPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(Constants.VOD_TEST_URL_STEVE)))
        newPlayer.prepare()
        newPlayer.play()
        delay(10_000)

        Log.d("ENABLEDISABLE", "re-enabling with new MediaItem (or 2nd video change)")
        // debugging: stop() the player => play,playing,pause,...,[actual start]
        // debugging: don't stop() the player => play,...,rebufferstart,....[actual start]
        // both cases should not sent playing (should result in 'starting up' always
        if (stopBeforeThirdVideo) {
          newPlayer.stop()
        }

        // with the resetState() method: not-calling stop() will work

        newPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(Constants.VOD_TEST_URL_BIG_BUCK_BUNNY)))
        if (!useEnable) {
          Log.d("ENABLEDISABLE", "calling videoChange() 2")
          muxStats?.videoChange(CustomerVideoData().apply {
            videoTitle = "Big Buck Bunny (Third)"
          })
        } else {
          Log.d("ENABLEDISABLE", "calling enable()")
          muxStats?.enable(CustomerData().apply {
            customerVideoData = CustomerVideoData().apply {
              videoTitle = "Big Buck Bunny (Third)"
            }
          })
        }
        Log.d("ENABLEDISABLE", "About to prepare the player. The current state is ${newPlayer.playbackState}")
        newPlayer.prepare()
        Log.d("ENABLEDISABLE", "Just called prepare on the player. The current state is ${newPlayer.playbackState}")
        newPlayer.play()
        // debugging: Maybe try calling enable() _after_

        delay(10_000)
        Log.d("ENABLEDISABLE", "test over")
        muxStats?.disable()
      }

      view.playerView.player = newPlayer
      newPlayer.setMediaItem(createMediaItem(mediaUrl))
      newPlayer.prepare()
      newPlayer.playWhenReady = true
    }
  }

  private fun createMediaItem(mediaUrl: String): MediaItem {
    return mediaUrl.toMediaItem().buildUpon()
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle("Sample app, BasicPlayerActivity")
          .setDescription("A Basic test video")
          .build()
      ).build()
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
        videoId = "A Custom ID"
        videoTitle = "Sintel (First)"
      },
      CustomerViewData().apply { }
    )

    return player.monitorWithMuxData(
      context = this,
      envKey = Constants.MUX_DATA_ENV_KEY,
      customerData = customerData,
      playerView = view.playerView,
      logLevel = MuxDataSdk.LogcatLevel.DEBUG,
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
        })
      }
  }

  companion object {
    const val EXTRA_URL: String = "com.mux.video.url"
  }
}
