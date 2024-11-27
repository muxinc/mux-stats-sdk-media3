package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource

/**
 * Returns true if the [MediaSource.MediaPeriodId] indicates that the period is in an ad break.
 *
 * Note the the default IMA and Media3 implementation report changing back to the main content
 * format before the ad break is over.
 */
@OptIn(UnstableApi::class)
fun MediaSource.MediaPeriodId.isInAdGroup(): Boolean {
  return this.adGroupIndex != C.INDEX_UNSET || this.adIndexInAdGroup != C.INDEX_UNSET
}
