package com.mux.stats.muxdatasdkformedia3.examples.ima

import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C.CONTENT_TYPE_HLS
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource.AdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionUriBuilder
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerSsaiBinding
import com.mux.stats.sdk.core.events.playback.AdEndedEvent
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.media3_ima.monitorWith
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData

class ImaServerAdsActivity : AppCompatActivity() {

  private lateinit var view: ActivityPlayerSsaiBinding
  private var player: Player? = null
  private var muxStats: MuxStatsSdkMedia3<ExoPlayer>? = null
  private var adsLoader: AdsLoader? = null
  private var adsLoaderState: AdsLoader.State? = null

  @OptIn(UnstableApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityPlayerSsaiBinding.inflate(layoutInflater)
    setContentView(view.root)

    view.playerView.apply {
      setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
      setControllerHideDuringAds(false)
      controllerAutoShow = true
    }
    view.skipAd.setOnClickListener {
      // Will end the ad break
      player?.seekForward()
    }
    window.addFlags(View.KEEP_SCREEN_ON)

    adsLoaderState = savedInstanceState?.getBundle(EXTRA_ADS_LOADER_STATE)
      ?.let { AdsLoader.State.CREATOR.fromBundle(it) }
  }

  override fun onResume() {
    super.onResume()
    startPlaying(
      Constants.AD_TAG_COMPLEX,
      null
    )
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  @OptIn(UnstableApi::class)
  override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
    stopPlaying()
    adsLoaderState?.let {
      outState.putBundle(EXTRA_ADS_LOADER_STATE, it.toBundle())
    }

    super.onSaveInstanceState(outState, outPersistentState)
  }

  @OptIn(UnstableApi::class)
  private fun createAdsLoaderIfNull(
    state: AdsLoader.State?,
    playerView: PlayerView,
  ): AdsLoader {
    return adsLoader
      ?: AdsLoader.Builder(this, playerView)
        .apply { state?.let { adsLoaderState = it } }
        .monitorWith(
          { muxStats }, // This is safe, will only be called while playing
          {
            /*your ad event handling here*/
          when (it.type) {
            AdEvent.AdEventType.AD_PERIOD_STARTED -> view.skipAd.visibility = View.VISIBLE
            AdEvent.AdEventType.AD_PERIOD_ENDED -> view.skipAd.visibility = View.GONE
            else -> { /* ignore */ }
          }
          },
          { /*your ad error handling here*/ },
        )
        .build()
  }

  @OptIn(UnstableApi::class)
  private fun startPlaying(
    adTagUri: String,
    adsLoader: AdsLoader?
  ) {
    player = if (player != null) {
      stopPlaying()
      player
    } else {
      @Suppress("NAME_SHADOWING") val adsLoader =
        adsLoader ?: createAdsLoaderIfNull(adsLoaderState, view.playerView)
      createPlayer(adsLoader).also { newPlayer ->
        muxStats = monitorPlayer(newPlayer)
        view.playerView.player = newPlayer
        adsLoader.setPlayer(newPlayer)

        val ssaiStreamUri = ImaServerSideAdInsertionUriBuilder()
          .setAssetKey(Constants.SSAI_ASSET_TAG_BUCK)
          .setFormat(CONTENT_TYPE_HLS)
          .build()

        newPlayer.setMediaItem(
          MediaItem.Builder()
            .setUri(ssaiStreamUri)
            .setAdsConfiguration(AdsConfiguration.Builder(Uri.parse(adTagUri)).build())
            .build()
        )
      }
    }
    player?.let {
      it.prepare()
      it.playWhenReady = true
    }
  }

  @OptIn(UnstableApi::class)
  private fun stopPlaying() {
    player?.let { oldPlayer ->
      oldPlayer.stop()
      oldPlayer.release()
    }
    // Make sure to release() your muxStats whenever the user is done with the player
    adsLoaderState = adsLoader?.release()
    muxStats?.release()
  }

  private fun monitorPlayer(player: ExoPlayer): MuxStatsSdkMedia3<ExoPlayer> {
    // You can add your own data to a View, which will override any data we collect
    val customerData = CustomerData(
      CustomerPlayerData().apply { },
      CustomerVideoData().apply {
        title = "Media3 IMA SSAI"
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
  private fun createPlayer(adsLoader: AdsLoader): ExoPlayer {
    //val mediaSrcFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this))
    val mediaSrcFactory = DefaultMediaSourceFactory { TestDataSourceSsai(DefaultDataSource.Factory(this)) }
    //.setLocalAdInsertionComponents({ adsLoader }, view.playerView)
    mediaSrcFactory.setServerSideAdInsertionMediaSourceFactory(
      ImaServerSideAdInsertionMediaSource.Factory(adsLoader, mediaSrcFactory)
    )

    return ExoPlayer.Builder(this)
      .setMediaSourceFactory(mediaSrcFactory)
      .build()
      .apply {
        addListener(object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            Log.e(javaClass.simpleName, "player error!", error)
            Toast.makeText(this@ImaServerAdsActivity, error.localizedMessage, Toast.LENGTH_SHORT)
              .show()
          }
        })
      }
  }

  companion object {
    const val EXTRA_ADS_LOADER_STATE = "ads loader state"
  }
}@OptIn(UnstableApi::class)
private class TestDataSourceSsai(
  val fac: DataSource.Factory
): BaseDataSource(true) {

  var httpDataSource: DataSource? = null
  var dataSpec: DataSpec? = null

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    if (failForTestingReasons(dataSpec!!) && false) {
      //throw Exception("failed")
      Thread.sleep(1000)
//      Thread.sleep(2000)
      return httpDataSource?.read(buffer, offset, length) ?: 0
    } else {
      return httpDataSource?.read(buffer, offset, length) ?: 0
    }
  }

  override fun open(dataSpec: DataSpec): Long {
    if (failForTestingReasons(dataSpec) && false) {
      //throw IOException("failed generically")
      //throw Exception("failed really generically")
      throw HttpDataSourceException("i died lol", dataSpec!!, PlaybackException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN)
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
    val daiHostname = "dai.google.com"
    // todo - ssai - i wonder if segments & manifests are handled differently
    return if (uri.host == daiHostname && uri.lastPathSegment?.endsWith(".ts") ?: false) {
      true
    } else {
      false
    }
  }

}