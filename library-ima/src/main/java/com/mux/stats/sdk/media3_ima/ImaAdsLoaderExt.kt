package com.mux.stats.sdk.media3_ima

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ima.ImaAdsLoader
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

/**
 * Monitors the [ImaAdsLoader] created by the given [ImaAdsLoader.Builder].
 *
 * Mux must take ownership of the [AdErrorListener] and [AdEventListener] for this ads loader, but
 * you can provide your own listeners and logic using the provided optional params
 *
 * @param muxStats The [MuxStatsSdkMedia3] instance monitoring your player
 * @param customerAdEventListener Optional. An [AdEventListener] with your apps custom ad-event handling
 * @param customerAdErrorListener Optional. An [AdErrorListener] containing your app's ad-error handling
 */
@OptIn(UnstableApi::class)
@JvmSynthetic
fun ImaAdsLoader.Builder.monitorWith(
  muxStats: MuxStatsSdkMedia3<*>,
  customerAdEventListener: AdEventListener = AdEventListener { },
  customerAdErrorListener: AdErrorListener = AdErrorListener { },
): ImaAdsLoader.Builder {
  val adsListener = MuxImaAdsListener.newListener(
    { muxStats },
    customerAdEventListener,
    customerAdErrorListener
  )

  setAdEventListener(adsListener)
  setAdErrorListener(adsListener)

  return this
}
