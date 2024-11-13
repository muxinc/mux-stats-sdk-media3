package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.Ad
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer.VideoAdPlayerCallback
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.mux.android.util.oneOf
import com.mux.stats.sdk.core.events.playback.*
import com.mux.stats.sdk.core.model.AdData
import com.mux.stats.sdk.core.model.ViewData
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.AdCollector
import com.mux.stats.sdk.muxstats.MuxPlayerState
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.core.events.playback.AdEvent as MuxAdEvent
import com.mux.stats.sdk.core.events.playback.AdErrorEvent as MuxAdErrorEvent

/**
 * Listens for [AdErrorEvent] and [AdEvent]s from an IMA Ads loader.
 */
class MuxImaAdsListener private constructor(
  private val provider: Provider,
  private val customerAdEventListener: AdEventListener = AdEventListener { },
  private val customerAdErrorListener: AdErrorListener = AdErrorListener { },
  private val customerVideoAdPlayerCallback: VideoAdPlayerCallback? = null,
) : AdErrorListener, AdEventListener, VideoAdPlayerCallback {

  /** The ExoPlayer that is playing the ads */
  private val exoPlayer: Player? get() = provider.boundPlayer
  private val adCollector: AdCollector? get() = provider.adCollector

  /** This value is used to detect if the user pressed the pause button when an ad was playing  */
  private var sendPlayOnStarted = false

  /**
   * This value is used in the special case of pre roll ads playing. This value will be set to
   * true when a pre roll is detected, and will be reverted back to false after dispatching the
   * AdBreakStart event.
   */
  private var missingAdBreakStartEvent = false

  /**
   * Handles VAST and other manifest-fetching errors
   *
   * @param adErrorEvent, Error to be handled.
   */
  override fun onAdError(adErrorEvent: AdErrorEvent) {
    val event = MuxAdErrorEvent(null)
    setupAdViewData(event, null)
    adCollector?.dispatch(event)
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
    val adData = AdData()
    if (ad != null) {
      adCollector?.let {
        if (it.playbackPositionMillis < 1000L) {
          viewData.viewPrerollAdId = ad.adId
          viewData.viewPrerollCreativeId = ad.creativeId
        }
      }

      exoPlayer?.getAdTagUrl()?.let { adData.adTagUrl = it }
      ad.adId?.let { adData.adId = it }
      ad.creativeId?.let { adData.adCreativeId = it }
      @Suppress("DEPRECATION") // This is only deprecated on android, we need consistency
      ad.universalAdIdValue?.let { adData.adUniversalId = it }
    }
    event.viewData = viewData
    event.adData = adData
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
      when (adEvent.type) {
        AdEvent.AdEventType.LOADED -> {
          // note that this event only means "data is available" and can be fired multiple times
          //  for the same successful ad. VideoPlayerAdCallback can be more reliable
        }

        AdEvent.AdEventType.LOG -> {
          val data = adEvent.adData
          // theoretically LOG could be for things other than errors so at least do this check
          if (data["errorMessage"] != null
            || data["errorCode"] != null
            || data["innerError"] != null
          ) {
            dispatchAdPlaybackEvent(MuxAdErrorEvent(null), adEvent.ad)
          } else {
            MuxLogger.d(TAG, "Logged IMA event: $adEvent")
          }
        }

        AdEvent.AdEventType.CONTENT_PAUSE_REQUESTED, // for CSAI
        AdEvent.AdEventType.AD_PERIOD_STARTED // for SSAI
        -> {
          @Suppress("RedundantNullableReturnType") val ad: Ad? = adEvent.ad
          // Send pause event if we are currently playing or preparing to play content
          if (adCollector?.muxPlayerState.oneOf(MuxPlayerState.PLAY, MuxPlayerState.PLAYING)) {
            adCollector?.onPausedForAds()
          }
          sendPlayOnStarted = false
          adCollector?.onStartPlayingAds()
          if (player.playWhenReady || player.currentPosition != 0L) {
            dispatchAdPlaybackEvent(AdBreakStartEvent(null), ad)
            dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
          } else {
            // This is preroll ads when play when ready is set to false, we need to ignore these events
            missingAdBreakStartEvent = true
          }
        }

        AdEvent.AdEventType.STARTED -> {
          // On the first STARTED, do not send AdPlay, as it was handled in
          // CONTENT_PAUSE_REQUESTED
          @Suppress("RedundantNullableReturnType") val ad: Ad? = adEvent.ad
          if (sendPlayOnStarted) {
            dispatchAdPlaybackEvent(AdPlayEvent(null), ad)
          } else {
            sendPlayOnStarted = true
          }
          dispatchAdPlaybackEvent(AdPlayingEvent(null), ad)
        }

        AdEvent.AdEventType.FIRST_QUARTILE -> {
          @Suppress("RedundantNullableReturnType") val ad: Ad? = adEvent.ad
          dispatchAdPlaybackEvent(
            AdFirstQuartileEvent(null),
            ad
          )
        }

        AdEvent.AdEventType.MIDPOINT -> {
          @Suppress("RedundantNullableReturnType") val ad: Ad? = adEvent.ad
          dispatchAdPlaybackEvent(AdMidpointEvent(null), ad)
        }

        AdEvent.AdEventType.THIRD_QUARTILE -> {
          @Suppress("RedundantNullableReturnType") // can be null during ssai
          val ad: Ad? = adEvent.ad
          dispatchAdPlaybackEvent(
            AdThirdQuartileEvent(null),
            ad
          )
        }

        AdEvent.AdEventType.COMPLETED -> {
          @Suppress("RedundantNullableReturnType") // can be null during ssai
          val ad: Ad? = adEvent.ad
          dispatchAdPlaybackEvent(AdEndedEvent(null), ad)
        }

        AdEvent.AdEventType.CONTENT_RESUME_REQUESTED, // for CSAI
        AdEvent.AdEventType.AD_PERIOD_ENDED // for SSAI
        -> {
          @Suppress("RedundantNullableReturnType") // can be null during ssai
          val ad: Ad? = adEvent.ad
          dispatchAdPlaybackEvent(AdBreakEndEvent(null), ad)
          // ExoPlayer state doesn't change for client ads so fill in the blanks
          val willPlayImmediately = player.playWhenReady
                  && player.playbackState == Player.STATE_READY
          adCollector?.onFinishPlayingAds(willPlayImmediately)
        }

        AdEvent.AdEventType.PAUSED -> {
          // This is preroll ads when play when ready is set to false, we need to ignore these events
          if (player.playWhenReady || player.currentPosition != 0L) {
            @Suppress("RedundantNullableReturnType") // can be null during ssai
            val ad: Ad? = adEvent.ad
            dispatchAdPlaybackEvent(AdPauseEvent(null), ad)
          } else {

          }
        }

        AdEvent.AdEventType.RESUMED -> {
          @Suppress("RedundantNullableReturnType") // can be null during ssai
          val ad: Ad? = adEvent.ad
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
        }

        else -> {}
      } // when()
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
    adCollector?.dispatch(event)
  }


  /** VideoAdPlayerCallback */
  override fun onAdProgress(mediaInfo: AdMediaInfo, progress: VideoProgressUpdate) {
    customerVideoAdPlayerCallback?.onAdProgress(mediaInfo, progress)
  }

  override fun onBuffering(mediaInfo: AdMediaInfo) {
    customerVideoAdPlayerCallback?.onBuffering(mediaInfo)
  }

  override fun onContentComplete() {
    customerVideoAdPlayerCallback?.onContentComplete()
  }

  override fun onEnded(mediaInfo: AdMediaInfo) {
    customerVideoAdPlayerCallback?.onEnded(mediaInfo)
  }

  override fun onError(mediaInfo: AdMediaInfo) {
    adCollector?.dispatch(MuxAdErrorEvent(null))
    customerVideoAdPlayerCallback?.onError(mediaInfo)
  }

  override fun onLoaded(mediaInfo: AdMediaInfo) {
    adCollector?.dispatch(AdResponseEvent(null))
    customerVideoAdPlayerCallback?.onLoaded(mediaInfo)
  }

  override fun onPause(mediaInfo: AdMediaInfo) {
    customerVideoAdPlayerCallback?.onPause(mediaInfo)
  }

  override fun onPlay(mediaInfo: AdMediaInfo) {
    customerVideoAdPlayerCallback?.onPlay(mediaInfo)
  }

  override fun onResume(mediaInfo: AdMediaInfo) {
    customerVideoAdPlayerCallback?.onResume(mediaInfo)
  }

  override fun onVolumeChanged(mediaInfo: AdMediaInfo, p1: Int) {
    customerVideoAdPlayerCallback?.onVolumeChanged(mediaInfo, p1)
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
      customerVideoAdPlayerCallback: VideoAdPlayerCallback? = null,
    ): MuxImaAdsListener {
      return MuxImaAdsListener(
        Provider { muxSdk },
        customerAdEventListener,
        customerAdErrorListener,
        customerVideoAdPlayerCallback
      )
    }
    /**
     * Creates a new [MuxImaAdsListener] based on the given [MuxStatsSdkMedia3]
     */
    @JvmStatic
    fun newListener(
      muxSdkProvider:() -> MuxStatsSdkMedia3<*>?,
      customerAdEventListener: AdEventListener = AdEventListener { },
      customerAdErrorListener: AdErrorListener = AdErrorListener { },
    ): MuxImaAdsListener {
      return MuxImaAdsListener(
        Provider { muxSdkProvider() },
        customerAdEventListener,
        customerAdErrorListener
      )
    }
  }
}

private class Provider(val provider: () -> MuxStatsSdkMedia3<*>?) {
  val boundPlayer get() = provider.invoke()?.boundPlayer
  val adCollector get() = provider.invoke()?.adCollector
}
