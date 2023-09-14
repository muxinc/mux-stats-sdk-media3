package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.Ad
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.mux.android.util.oneOf
import com.mux.android.util.weak
import com.mux.stats.sdk.core.events.playback.*
import com.mux.stats.sdk.core.model.AdData
import com.mux.stats.sdk.core.model.ViewData
import com.mux.stats.sdk.muxstats.AdCollector
import com.mux.stats.sdk.muxstats.MuxPlayerState
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.core.events.playback.AdEvent as MuxAdEvent

/**
 * Listens for [AdErrorEvent] and [AdEvent]s from an IMA Ads loader.
 */
class MuxImaAdsListener private constructor(
  exoPlayer: Player,
  private val adCollector: AdCollector,
  private val customerAdEventListener: AdEventListener = AdEventListener { },
  private val customerAdErrorListener: AdErrorListener = AdErrorListener { },
) : AdErrorListener, AdEventListener {

  /** The ExoPlayer that is playing the ads */
  private val exoPlayer by weak(exoPlayer)

  /** This value is used to detect if the user pressed the pause button when an ad was playing  */
  private var sendPlayOnStarted = false

  /**
   * This value is used in the special case of pre roll ads playing. This value will be set to
   * true when a pre roll is detected, and will be reverted back to false after dispatching the
   * AdBreakStart event.
   */
  private var missingAdBreakStartEvent = false

  /**
   * Handles Ad errors
   *
   * @param adErrorEvent, Error to be handled.
   */
  override fun onAdError(adErrorEvent: AdErrorEvent) {
    val event = AdErrorEvent(null)
    setupAdViewData(event, null)
    adCollector.dispatch(event)
    customerAdErrorListener.onAdError(adErrorEvent)
  }

  /**
   * Update the adId and creativeAdId on given playback event.
   *
   * @param event, event to be updated.
   * @param ad, current ad event that is being processed.
   */
  private fun setupAdViewData(event: MuxAdEvent, ad: Ad?) {
    val viewData = ViewData()
    val adData = AdData();
    if (adCollector.playbackPositionMillis == 0L) {
      if (ad != null) {
        viewData.viewPrerollAdId = ad.adId
        viewData.viewPrerollCreativeId = ad.creativeId

        exoPlayer?.getAdTagUrl()?.let { adData.adTagUrl = it }
        ad.adId?.let { adData.adId = it }
        ad.creativeId?.let { adData.adCreativeId = it }
        ad.universalAdIdValue?.let { adData.adUniversalId = it }
      }
    }
    event.viewData = viewData
    event.adData = adData;
  }

  /**
   * This is the main boilerplate, all processing logic is contained here. Depending on the phase
   * the ad is in (a single ad can have multiple phases from loading to the ending) each of them
   * is handled here and an appropriate AdPlayback event is dispatched to the backend.
   *
   * @param adEvent
   */
  override fun onAdEvent(adEvent: AdEvent) {
    exoPlayer?.let { player ->
      val event: PlaybackEvent? = null
      val ad = adEvent.ad
      when (adEvent.type) {
        AdEvent.AdEventType.LOADED -> {}
        AdEvent.AdEventType.CONTENT_PAUSE_REQUESTED -> {
          // Send pause event if we are currently playing or preparing to play content
          if (adCollector.muxPlayerState.oneOf(MuxPlayerState.PLAY, MuxPlayerState.PLAYING)) {
            adCollector.onPausedForAds()
          }
          sendPlayOnStarted = false
          adCollector.onStartPlayingAds()
          if (!player.playWhenReady && player.currentPosition == 0L) {
            // This is preroll ads when play when ready is set to false, we need to ignore these events
            missingAdBreakStartEvent = true
            return
          }
          dispatchAdPlaybackEvent(AdBreakStartEvent(null), ad)
          dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
        }

        AdEvent.AdEventType.STARTED -> {
          // On the first STARTED, do not send AdPlay, as it was handled in
          // CONTENT_PAUSE_REQUESTED
          if (sendPlayOnStarted) {
            dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
          } else {
            sendPlayOnStarted = true
          }
          dispatchAdPlaybackEvent(AdPlayingEvent(null), ad)
        }

        AdEvent.AdEventType.FIRST_QUARTILE -> dispatchAdPlaybackEvent(
          AdFirstQuartileEvent(null),
          ad
        )

        AdEvent.AdEventType.MIDPOINT -> dispatchAdPlaybackEvent(AdMidpointEvent(null), ad)
        AdEvent.AdEventType.THIRD_QUARTILE -> dispatchAdPlaybackEvent(
          AdThirdQuartileEvent(null),
          ad
        )

        AdEvent.AdEventType.COMPLETED -> dispatchAdPlaybackEvent(AdEndedEvent(null), ad)
        AdEvent.AdEventType.CONTENT_RESUME_REQUESTED -> {
          dispatchAdPlaybackEvent(AdBreakEndEvent(null), ad)
          // ExoPlayer state doesn't change for client ads so fill in the blanks
          val willPlayImmediately = player.playWhenReady
                  && player.playbackState == Player.STATE_READY
          adCollector.onFinishPlayingAds(willPlayImmediately)
        }

        AdEvent.AdEventType.PAUSED -> {
          if (!player.playWhenReady
            && player.currentPosition == 0L
          ) {
            // This is preroll ads when play when ready is set to false, we need to ignore these events
            return;
          }
          dispatchAdPlaybackEvent(AdPauseEvent(null), ad)
        }

        AdEvent.AdEventType.RESUMED ->
          if (missingAdBreakStartEvent) {
            // This is special case when we have ad preroll and play when ready is set to false
            // in that case we need to dispatch AdBreakStartEvent first and resume the playback.
            dispatchAdPlaybackEvent(AdBreakStartEvent(null), ad)
            dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
            missingAdBreakStartEvent = false
          } else {
            dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
            dispatchAdPlaybackEvent(AdPlayingEvent(null), ad)
          }
        else -> { }
      } // when (adEvent.type)
    } // exoPlayer?.let ...

    customerAdEventListener.onAdEvent(adEvent)
  }

  /**
   * Prepare and dispatch the given playback event to the backend using @link exoPlayerListener.
   *
   * @param event, to be dispatched.
   * @param ad, ad being processed.
   */
  private fun dispatchAdPlaybackEvent(event: MuxAdEvent, ad: Ad?) {
    setupAdViewData(event, ad)
    adCollector.dispatch(event)
  }

  companion object {
    private const val TAG = "MuxImaAdsListener"

    /**
     * Creates a new [MuxImaAdsListener] based on the given [MuxStatsSdkMedia3]
     */
    @JvmStatic
    fun newListener(
      muxSdk: MuxStatsSdkMedia3<*>,
      customerAdEventListener: AdEventListener = AdEventListener { },
      customerAdErrorListener: AdErrorListener = AdErrorListener { },
    ): MuxImaAdsListener {
      return MuxImaAdsListener(
        muxSdk.boundPlayer,
        muxSdk.adCollector,
        customerAdEventListener,
        customerAdErrorListener
      )
    }
  }
}
