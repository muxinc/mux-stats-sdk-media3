package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.common.Player
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.events.EventBus
import com.mux.stats.sdk.core.events.playback.AdEvent
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.muxstats.media3.BuildConfig

/**
 * TODO: doc out of date
 * Monitors a generic [Player] with Mux Data, reporting data about the View to the environment
 * specified by the env key.
 *
 * @param context a Context containing your player. If it's an Activity, screen size is obtained
 * @param envKey your Mux Data Environment Key
 * @param customerData data about you, your customer, and your video
 * @param player the player you wish to observe
 * @param playerView the View showing your video content
 * @param customOptions Options that affect the behavior of the SDK
 * @param playerBinding a [MuxPlayerAdapter.PlayerBinding] that can observe the state of your player
 * @param P The type of player being monitored.
 */
class MuxStatsSdkMedia3<P : Player> @JvmOverloads constructor(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  player: P,
  playerView: View? = null,
  customOptions: CustomOptions? = null,
  playerBinding: MuxPlayerAdapter.PlayerBinding<P>,
) : MuxDataSdk<P, View>(
  context = context,
  envKey = envKey,
  player = player,
  playerView = playerView,
  customerData = customerData,
  customOptions = customOptions ?: CustomOptions(),
  logLevel = LogcatLevel.DEBUG,
  trackFirstFrame = true,
  playerBinding = playerBinding,
  device = AndroidDevice(
    ctx = context,
    playerVersion = BuildConfig.MEDIA3_VERSION, /* TODO: Dynamic would be better if possible*/
    muxPluginName = "mux-media3",
    muxPluginVersion = BuildConfig.LIB_VERSION,
    playerSoftware = "media3-generic",
  )
) {
  /**
   * Collects events related to ad playback and reports them. If you are using Google IMA, you don't
   * need to interact with this class directly. Instead, use the `media3-ima` library provided by
   * Mux (TODO: Doc link)
   */
  val adCollector by lazy { AdCollector.create(collector, eventBus) }

  /**
   * The player bound to this object
   */
  val boundPlayer: P get() { return player }
}

// TODO: boundPlayer/AdCollector on core-android

/**
 * Collects generic data and events regarding ad playback.
 *
 * If you're using the Google IMA Ads SDK, can use MuxImaAdsListener in our `media3-ima` lib
 * (TODO: Doc link)
 */
class AdCollector private constructor(
  private val stateCollector: MuxStateCollector,
  private val eventBus: EventBus
) {

  /**
   * The current playback position
   */
  val playbackPositionMillis get() = stateCollector.playbackPositionMills

  /**
   * The state of the player as understood by Mux
   */
  val muxPlayerState get() = stateCollector.muxPlayerState

  /**
   * Call when playback pauses for ads
   */
  fun onPausedForAds() {
    stateCollector.pause()
  }

  /**
   * Call when ad playback starts
   */
  fun onStartPlayingAds() {
    stateCollector.playingAds()
  }

  /**
   * Call when done playing ads
   */
  fun onFinishPlayingAds() {
    stateCollector.finishedPlayingAds()
  }

  fun dispatch(event: AdEvent) {
    eventBus.dispatch(event)
  }

  companion object {
    @JvmSynthetic
    internal fun create(collector: MuxStateCollector, eventBus: EventBus): AdCollector {
      return AdCollector(collector, eventBus)
    }
  }
}
