package com.mux.stats.muxdatasdkformedia3.examples.ima

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.media3_ima.monitorWith
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData
import java.io.IOException

class ImaClientAdsActivity : AppCompatActivity() {

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
      controllerAutoShow = true
    }
    window.addFlags(View.KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    startPlaying(Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY, Constants.AD_TAG_COMPLEX)
//    startPlaying(Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY, Constants.AD_TAG_SIMPLE_W_MID_ROLL)
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  @OptIn(UnstableApi::class)
  private fun startPlaying(mediaUrl: String, adTagUri: String) {
    stopPlaying()

    player = createPlayer().also { newPlayer ->
      muxStats = monitorPlayer(newPlayer)
      adsLoader = ImaAdsLoader.Builder(this)
        .monitorWith(
          muxStats = muxStats!!,
          customerAdErrorListener = { /*Optional parameter, your custom logic*/ },
          customerAdEventListener = { /*Optional parameter, your custom logic*/ },
        )
        .build()
        .apply { setPlayer(newPlayer) }

      view.playerView.player = newPlayer
      newPlayer.setMediaItem(
        MediaItem.Builder()
          .setUri(Uri.parse(mediaUrl))
          .setAdsConfiguration(AdsConfiguration.Builder(Uri.parse(adTagUri)).build())
          .build()
      )
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
        title = "Media3 IMA CSAI"
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

  @OptIn(UnstableApi::class)
  private fun createPlayer(): ExoPlayer {
//    val mediaSrcFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this))
    val mediaSrcFactory = DefaultMediaSourceFactory { TestDataSource(DefaultDataSource.Factory(this)) }
      .setLocalAdInsertionComponents({ adsLoader }, view.playerView)

    return ExoPlayer.Builder(this)
      .setMediaSourceFactory(mediaSrcFactory)
      .build()
      .apply {
        addListener(object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            Log.e(javaClass.simpleName, "player error!", error)
            Toast.makeText(this@ImaClientAdsActivity, error.localizedMessage, Toast.LENGTH_SHORT).show()
          }
        })
      }
  }
}

@OptIn(UnstableApi::class)
private class TestDataSource(
  val fac: DataSource.Factory
): BaseDataSource(true) {

  var httpDataSource: DataSource? = null
  var dataSpec: DataSpec? = null

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    if (failForTestingReasons(dataSpec!!) && false) {
      throw Exception("failed")
    } else {
      return httpDataSource?.read(buffer, offset, length) ?: 0
    }
  }

  override fun open(dataSpec: DataSpec): Long {
    if (failForTestingReasons(dataSpec)) {
      //throw IOException("failed generically")
      throw Exception("failed really generically")
    } else {
      val src = fac.createDataSource()
      this.httpDataSource = src
      this.dataSpec = dataSpec
      return src.open(dataSpec)
    }
   }

  override fun getUri(): Uri? {
    return httpDataSource?.uri
  }

  override fun close() {
    httpDataSource?.close()
  }

  private fun failForTestingReasons(dataSpec: DataSpec): Boolean {
    val uri = dataSpec.uri

    // ima csai sample
    val filename = "file.mp4"
    // todo - ssai - i wonder if segments & manifests are handled differently
    return if (uri.lastPathSegment == filename) {
      true
    } else {
      false
    }
  }

}