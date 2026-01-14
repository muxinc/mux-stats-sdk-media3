package com.mux.stats.sdk.media3_ima

import com.google.ads.interactivemedia.v3.api.Ad

/**
 * Compatibility layer for Media3 IMA versions 1.0 through 1.8
 */
internal object ImaCompat {

  /**
   * Return a universalAdId using an API available on this media3 version
   *
   * This implementation uses [Ad.getUniversalAdIdValue] since Mux Data supports only one
   * universal ad ID. The API is deprecated for modern IMA versions, but we use it where available
   * for consistency with the other platforms
   */
  @Suppress("RedundantNullableReturnType")
    fun universalAdId(ad: Ad): String? {
      @Suppress("DEPRECATION") // deprecated on some versions
      return ad.universalAdIdValue
    }
}
