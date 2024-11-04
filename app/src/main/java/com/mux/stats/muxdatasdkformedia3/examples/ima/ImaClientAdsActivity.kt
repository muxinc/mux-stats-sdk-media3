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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityImaClientAdsBinding
import com.mux.stats.muxdatasdkformedia3.examples.ImaAdTags
import com.mux.stats.muxdatasdkformedia3.examples.ImaClientAdsParamHelper
import com.mux.stats.muxdatasdkformedia3.view.SpinnerParamEntryView
import com.mux.stats.sdk.core.model.CustomData
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.media3_ima.monitorWith
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData

class ImaClientAdsActivity : AppCompatActivity() {

  private lateinit var view: ActivityImaClientAdsBinding
  private var player: Player? = null
  private var muxStats: MuxStatsSdkMedia3<ExoPlayer>? = null
  private var adsLoader: ImaAdsLoader? = null

  private val paramHelper = ImaClientAdsParamHelper()

  @OptIn(UnstableApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityImaClientAdsBinding.inflate(layoutInflater)
    setContentView(view.root)

    savedInstanceState?.let { paramHelper.restoreInstanceState(it) }

    view.configurablePlayerSrcUrl.onClear = {
      paramHelper.sourceUrl = null
    }

    // todo - rename layout to adtagurl something
    view.configurablePlayerCustomDomain.onSelected = {
      val (title, adTagUrl) = view.configurablePlayerCustomDomain.entry
      paramHelper.adTagUrl = adTagUrl
      paramHelper.title = title

      // todo - repeated later on
      val customerData = CustomerData(
        CustomerPlayerData().apply { },
        CustomerVideoData().apply {
          videoTitle = "Mux Data for Media3 - IMA Ads"
        },
        CustomerViewData().apply { },
        CustomData().apply {
          customData1 = paramHelper.adTagUrl
          customData2 = paramHelper.title
        }
      )
      muxStats?.updateCustomerData(customerData)

    }
    // todo - the 'update ad tag url' button
    view.configurablePlayerCustomDomain.adapter = createAdTagAdapter()
    view.playerView.apply {
      setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
      controllerAutoShow = true
    }

    window.addFlags(View.KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    startPlaying(Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY, Constants.AD_TAG_COMPLEX)
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    paramHelper.saveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  private fun createAdTagAdapter(): SpinnerParamEntryView.Adapter {
    val googleTags = ImaAdTags.googleTags.map { tag ->
      SpinnerParamEntryView.Item(
        customAllowed = false,
        title = tag.title,
        text = tag.adTagUrl
      )
    }
    val customTag = SpinnerParamEntryView.Item(
      customAllowed = true,
      title = "Custom Ad Tag",
      text = null,
    )
    val allTags = listOf(customTag) + googleTags

    return view.configurablePlayerCustomDomain.Adapter(this, allTags)
  }

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
      newPlayer.setMediaItem(paramHelper.createMediaItem())
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
        videoTitle = "Mux Data for Media3 - IMA Ads"
      },
      CustomerViewData().apply { },
      CustomData().apply {
        customData1 = paramHelper.adTagUrl
        customData2 = paramHelper.title
      }
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
    val mediaSrcFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this))
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