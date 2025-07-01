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

fun trackTypeString(trackType: Int): String = when (trackType) {
  C.TRACK_TYPE_NONE -> "none"
  C.TRACK_TYPE_UNKNOWN -> "unknown"
  C.TRACK_TYPE_DEFAULT -> "default"
  C.TRACK_TYPE_AUDIO -> "audio"
  C.TRACK_TYPE_VIDEO -> "video"
  C.TRACK_TYPE_TEXT -> "text"
  C.TRACK_TYPE_IMAGE -> "image"
  C.TRACK_TYPE_METADATA -> "metadata"
  C.TRACK_TYPE_CAMERA_MOTION -> "camera_motion"
  else -> "other: $trackType"
}

@OptIn(UnstableApi::class)
fun dataTypeString(dataType: Int): String = when (dataType) {
  C.DATA_TYPE_UNKNOWN -> "unkown"
  C.DATA_TYPE_MEDIA -> "media"
  C.DATA_TYPE_MEDIA_INITIALIZATION -> "media_initialization"
  C.DATA_TYPE_DRM -> "drm"
  C.DATA_TYPE_MANIFEST -> "manifest"
  C.DATA_TYPE_TIME_SYNCHRONIZATION -> "time_sycnhronization"
  C.DATA_TYPE_AD -> "ad"
  C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> "progressive_live"
  else -> "other: $dataType"
}
