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
  C.TRACK_TYPE_NONE -> "TRACK_TYPE_NONE"
  C.TRACK_TYPE_UNKNOWN -> "TRACK_TYPE_UNKNOWN"
  C.TRACK_TYPE_DEFAULT -> "TRACK_TYPE_DEFAULT"
  C.TRACK_TYPE_AUDIO -> "TRACK_TYPE_AUDIO"
  C.TRACK_TYPE_VIDEO -> "TRACK_TYPE_VIDEO"
  C.TRACK_TYPE_TEXT -> "TRACK_TYPE_TEXT"
  C.TRACK_TYPE_IMAGE -> "TRACK_TYPE_IMAGE"
  C.TRACK_TYPE_METADATA -> "TRACK_TYPE_METADATA"
  C.TRACK_TYPE_CAMERA_MOTION -> "TRACK_TYPE_CAMERA_MOTION"
  else -> "other: $trackType"
}

@OptIn(UnstableApi::class)
fun dataTypeString(dataType: Int): String = when (dataType) {
  C.DATA_TYPE_UNKNOWN -> "DATA_TYPE_UNKNOWN"
  C.DATA_TYPE_MEDIA -> "DATA_TYPE_MEDIA"
  C.DATA_TYPE_MEDIA_INITIALIZATION -> "DATA_TYPE_MEDIA_INITIALIZATION"
  C.DATA_TYPE_DRM -> "DATA_TYPE_DRM"
  C.DATA_TYPE_MANIFEST -> "DATA_TYPE_MANIFEST"
  C.DATA_TYPE_TIME_SYNCHRONIZATION -> "DATA_TYPE_TIME_SYNCHRONIZATION"
  C.DATA_TYPE_AD -> "DATA_TYPE_AD"
  C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> "DATA_TYPE_MEDIA_PROGRESSIVE_LIVE"
  else -> "other: $dataType"
}
