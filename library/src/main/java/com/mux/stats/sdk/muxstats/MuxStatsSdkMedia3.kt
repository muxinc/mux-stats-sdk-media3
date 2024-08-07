package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.common.Player
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.events.EventBus
import com.mux.stats.sdk.core.events.playback.AdEvent
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.muxstats.media3.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
 * @param network Optional. A custom [INetworkRequest] to use instead of the default.
 * @param device Optional. A custom [IDevice] to use instead of the default.
 * @param playerBinding a [MuxPlayerAdapter.PlayerBinding] that can observe the state of your player
 * @param logLevel The log level to use.
 * @param P The type of player being monitored.
 */
class MuxStatsSdkMedia3<P : Player> @JvmOverloads constructor(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  player: P,
  playerView: View? = null,
  customOptions: CustomOptions? = null,
  network: INetworkRequest? = null,
  device: IDevice? = null,
  logLevel: LogcatLevel = LogcatLevel.NONE,
  playerBinding: MuxPlayerAdapter.PlayerBinding<P>,
) : MuxDataSdk<P, View>(
  context = context,
  envKey = envKey,
  player = player,
  playerView = playerView,
  customerData = customerData,
  customOptions = customOptions ?: CustomOptions(),
  logLevel = logLevel,
  trackFirstFrame = true,
  playerBinding = playerBinding,
  device = device ?: AndroidDevice(
    ctx = context,
    playerVersion = BuildConfig.MEDIA3_VERSION, /* TODO: dynamically detecting would be better*/
    muxPluginName = "mux-media3",
    muxPluginVersion = BuildConfig.LIB_VERSION,
    playerSoftware = "media3-generic",
  ),
  makeNetworkRequest = { iDevice -> network ?: MuxNetwork(iDevice, CoroutineScope(Dispatchers.IO)) }
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

  /*
   * Ok so the problem is, probably, that we don't have
   *
   * So it's not necessarily that we are getting old data, it's really more that the monitor has
   * stale state data. Like, we dispatch rebufferend because we get playing() and we mistook the
   * BUFFERING from changing items as simple rebuffering. What we want to send in response to that
   * change to BUFFERING, is 'play'. Then 'playing' later doesn't send rebufferend.
   *
   * It looks like that case is entering BUFFERING while MSC is in PLAYING, so we definitely start
   * rebuffering here. We want to be in the 'buffering' state, and send 'play'
   * We need to reset the MSC when we get enable()
   *
   * Yeah, definitely that helped with the case where the player was not stopped
   * It may have also helped with the case where the player was stopped but I couldn't reproduce
   * that case anyway
   *
   * oh wow videoChange here does crazy things. How long as this been like this? We have tests in
   * the player lib for this I thought
   *
   * Anyway, we can catch-up state on videoChange also and that seems to make videoChange better
   * Customers have to call videoChange() after setting the new MediaItem(s), which I think is fine
   *
   * And you have to call enable() or videoChange() *after* setting the MediaItem, or else you'll
   * capture the old MediaItem's play state.
   *  (this isn't a problem for the 'old' mode of operation because the player was not reused)
   *
   * So like we need to change the documented usage too. Something like, "Reusing your player and
   * monitor for multiple MediaItems," where we describe `videoChange` and `disable/enable` and
   * explain how the usage is different when you reuse your player.
   */

  override fun enable(customerData: CustomerData) {
    // call-through to start the new view
    super.enable(customerData)
    // catch-up player state in case we missed prepare()
    catchUpPlayState(player, collector)
    catchUpStreamData(player, collector)
  }

  override fun videoChange(videoData: CustomerVideoData) {
    super.videoChange(videoData)
    catchUpPlayState(player, collector)
    catchUpStreamData(player, collector)
  }
}

/**
 * Collects generic data and events regarding ad playback.
 *
 * If you're using the Google IMA Ads SDK, can use MuxImaAdsListener in our `media3-ima` lib
 * (TODO: Doc link)
 */
class AdCollector private constructor(
  private val stateCollector: MuxStateCollector,
  private val eventBus: EventBus,
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
    // edge case: if an IMA mid/postroll ad fails to load in time, the ExoPlayer will go into the
    //   BUFFERING state and we'll be in REBUFFERING, for one final attempt to get the ads.
    //   If that fails, the player will go into an adbreak for a few millis and then continue with
    //   content. The events all really happen and there really is a short playback interuption so
    //   it's good to count it all
    if (muxPlayerState == MuxPlayerState.REBUFFERING) {
      stateCollector.pause()
    }

    stateCollector.playingAds()
  }

  /**
   * Call when done playing ads
   */
  fun onFinishPlayingAds(willPlay: Boolean) {
    stateCollector.finishedPlayingAds()
    if(willPlay) {
      stateCollector.playing()
    }
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
