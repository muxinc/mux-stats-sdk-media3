package com.mux.stats.sdk.media3_ima

import com.google.ads.interactivemedia.v3.api.Ad

/**
 * Compatibility layer for Media3 IMA versions 1.9 and later
 */
internal object ImaCompat {

  /**
   * Return a universalAdId using an API available on this media3 version
   *
   * This implementation returns the first universalAdId, since Mux Data supports only oneyy
   */
  fun universalAdId(ad: Ad): String? {
    return ad.universalAdIds.takeIf { it.isNotEmpty() }?.firstOrNull()?.adIdValue
  }
}