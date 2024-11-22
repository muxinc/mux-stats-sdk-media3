package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource

/**
 * Returns true if the [MediaSource.MediaPeriodId] indicates that the period is in an ad break.
 *
 * Note that the default implementation for SSAI/DAI reports the input format late, doing so just
 * before switching off of the ad or content media. In this case, you can still use this method to
 * tell whether an input format change belongs to an ad period even if the timing is off.
 */
@OptIn(UnstableApi::class)
fun MediaSource.MediaPeriodId.isInAdPeriod(): Boolean {
  return this.adGroupIndex != C.INDEX_UNSET || this.adIndexInAdGroup != C.INDEX_UNSET
}
