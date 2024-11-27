package com.mux.stats.muxdatasdkformedia3.examples.ima

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
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
import com.mux.stats.sdk.muxstats.MuxDataSdk
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

    view.imaClientAdsSrcUrl.onClear = {
      paramHelper.sourceUrl = null
    }
    view.imaClientAdsSpinner.onSelected = {
      // update the ad tag whenever a new spinner item is selected
      val (title, adTagUrl) = view.imaClientAdsSpinner.entry
      // ... unless you need to enter text
      if (!adTagUrl.isNullOrBlank()) {
        paramHelper.adTagUrl = adTagUrl
        paramHelper.title = title
        val customerData = createCustomerData(title, adTagUrl)
        muxStats?.updateCustomerData(customerData)
        initPlayer(play = player?.isPlaying == true)
      }
    }
    view.imaClientAdsUpdateMediaItem.setOnClickListener {
      // update everything when the 'update' button is clicked
      paramHelper.sourceUrl = view.imaClientAdsSrcUrl.entry
      paramHelper.envKey = view.imaClientAdsDataKey.entry

      val (title, adTagUrl) = view.imaClientAdsSpinner.entry
      paramHelper.adTagUrl = adTagUrl
      val customerData = createCustomerData(title, adTagUrl)
      muxStats?.updateCustomerData(customerData)

      initPlayer(play = player?.isPlaying == true)
    }
    view.imaClientAdsSpinner.adapter = createAdTagAdapter()

    view.imaClientAdsSrcUrl.hint = ImaClientAdsParamHelper.DEFAULT_SOURCE_URL
    view.imaClientAdsSrcUrl.entry = paramHelper.sourceUrl
    view.imaClientAdsSrcUrl.onClear = { paramHelper.sourceUrl = null }

    view.imaClientAdsDataKey.hint = Constants.MUX_DATA_ENV_KEY
    view.imaClientAdsDataKey.entry = paramHelper.envKey
    view.imaClientAdsDataKey.onClear = { paramHelper.envKey = null }

    view.playerView.apply {
      setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
      controllerAutoShow = true
    }

    window.addFlags(View.KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onPause() {
    stopPlaying()
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    paramHelper.saveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  private fun createCustomerData(title: String?, adTagUrl: String?): CustomerData {
    return CustomerData(
      CustomerPlayerData().apply { },
      CustomerVideoData().apply {
        videoTitle = "Media3 - IMA: $title"
      },
      CustomerViewData().apply { },
      CustomData().apply {
        customData1 = adTagUrl
        customData2 = title
      }
    )
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

    return view.imaClientAdsSpinner.Adapter(this, allTags)
  }

  private fun initPlayer(play: Boolean = true) {
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
      if (play) { newPlayer.prepare() }
      newPlayer.playWhenReady = play
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
    val customerData = createCustomerData(paramHelper.title, paramHelper.adTagUrl)

    return player.monitorWithMuxData(
      context = this,
      logLevel = MuxDataSdk.LogcatLevel.DEBUG,
      envKey = paramHelper.envKeyOrDefault(),
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