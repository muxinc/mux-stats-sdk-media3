package com.mux.stats.sdk.media3_ima

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

/**
 * Monitors the [ImaServerSideAdInsertionMediaSource.AdsLoader] created by the a
 * [ImaServerSideAdInsertionMediaSource.AdsLoader.Builder].
 *
 * This method is just for DAI server-side ad-insertion. If you're doing Client-Side Ad Insertion
 * (CSAI), use [ImaAdsLoader.Builder.monitorWith]
 *
 * Mux must take ownership of the [AdErrorListener] and [AdEventListener] for this ads loader, but
 * you can provide your own listeners and logic using the provided optional params
 *
 * @param muxStatsProvider Provides [MuxStatsSdkMedia3] instance for monitoring your player.
 * @param customerAdEventListener Optional. An [AdEventListener] with your apps custom ad-event handling
 * @param customerAdErrorListener Optional. An [AdErrorListener] containing your app's ad-error handling
 */
@OptIn(UnstableApi::class)
@JvmSynthetic
fun AdsLoader.Builder.monitorWith(
  muxStatsProvider: () -> MuxStatsSdkMedia3<*>?,
  customerAdEventListener: AdEventListener = AdEventListener { },
  customerAdErrorListener: AdErrorListener = AdErrorListener { },
): AdsLoader.Builder {
  val adsListener = MuxImaAdsListener.newListener(
    muxStatsProvider,
    customerAdEventListener,
    customerAdErrorListener
  )

  setAdEventListener(adsListener)
  setAdErrorListener(adsListener)

  return this
}
