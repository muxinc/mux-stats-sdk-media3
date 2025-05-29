package com.mux.stats.sdk.muxstats

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.mux.android.util.weak
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.bandwidth.BandwidthMetricDispatcher
import com.mux.stats.sdk.muxstats.bandwidth.TrackedHeader
import com.mux.stats.sdk.muxstats.internal.createErrorDataBinding
import com.mux.stats.sdk.muxstats.internal.createExoSessionDataBinding
import com.mux.stats.sdk.muxstats.internal.isInAdGroup
import com.mux.stats.sdk.muxstats.internal.populateLiveStreamData
import java.io.IOException
import java.util.regex.Pattern

/**
 * [MuxPlayerAdapter.PlayerBinding] implementation for `ExoPlayer`.
 *
 * Java callers should provide a new instance of this class when creating their [MuxStatsSdkMedia3]
 * instance
 *
 * Kotlin callers shouldn't need to interact with this class. Just use [monitorWithMuxData] to
 * handle this automatically
 */
open class ExoPlayerBinding : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {

  private val sessionDataBinding = createExoSessionDataBinding()
  private val errorBinding = createErrorDataBinding()

  private var listener: MuxAnalyticsListener? = null

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    catchUpPlayState(player, collector)
    catchUpStreamData(player, collector)

    listener = MuxAnalyticsListener(
      player = player,
      collector = collector,
      bandwidthMetrics = BandwidthMetricDispatcher(
        player = player,
        collector = collector,
        trackedResponseHeaders = listOf(
          TrackedHeader.ExactlyIgnoreCase("x-cdn"),
          TrackedHeader.ExactlyIgnoreCase("content-type"),
          TrackedHeader.ExactlyIgnoreCase("x-request-id"),
          TrackedHeader.Matching(Pattern.compile("^x-litix-.*", Pattern.CASE_INSENSITIVE))
        )
      )
    ).also { player.addAnalyticsListener(it) }

    // Also delegate to sub-bindings
    errorBinding.bindPlayer(player, collector)
    sessionDataBinding.bindPlayer(player, collector)
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener?.let { player.removeAnalyticsListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null

    // Also delegate to sub-bindings
    sessionDataBinding.unbindPlayer(player, collector)
    errorBinding.unbindPlayer(player, collector)
  }

  companion object {
    @Suppress("unused")
    private const val TAG = "ExoPlayerBinding"
  }
}

@OptIn(UnstableApi::class)
private class MuxAnalyticsListener(
  player: ExoPlayer,
  val bandwidthMetrics: BandwidthMetricDispatcher,
  val collector: MuxStateCollector,
) : AnalyticsListener {

  private val player by weak(player)
  private var lastVideoFormat: Format? = null

  override fun onPlayWhenReadyChanged(
    eventTime: AnalyticsListener.EventTime,
    playWhenReady: Boolean,
    reason: Int
  ) {
    player?.let { collector.handlePlayWhenReady(playWhenReady, it.playbackState) }
  }

  override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
    // query playWhenReady for consistency. The order of execution between this callback and
    //  onPlayWhenReadyChanged is not well-defined
    player?.let { collector.handleExoPlaybackState(state, it.playWhenReady) }
  }

  override fun onPositionDiscontinuity(
    eventTime: AnalyticsListener.EventTime,
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int
  ) {
    collector.handlePositionDiscontinuity(reason)
  }

  override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime, reason: Int) {
    eventTime.timeline.takeIf { it.windowCount > 0 }?.let { tl ->
      val window = Timeline.Window().apply { tl.getWindow(0, this) }
      collector.sourceDurationMs = window.durationMs
      collector.populateLiveStreamData(window)
    }
  }

  override fun onMediaItemTransition(
    eventTime: AnalyticsListener.EventTime,
    mediaItem: MediaItem?,
    reason: Int
  ) {
    mediaItem?.let { collector.handleMediaItemChanged(it) }
  }

  override fun onMediaMetadataChanged(
    eventTime: AnalyticsListener.EventTime,
    mediaMetadata: MediaMetadata
  ) {
    @Suppress("UNNECESSARY_SAFE_CALL")
    mediaMetadata?.let { collector.handleMediaMetadata(it) }
  }

  override fun onVideoInputFormatChanged(
    eventTime: AnalyticsListener.EventTime,
    format: Format,
    decoderReuseEvaluation: DecoderReuseEvaluation?
  ) {
    MuxLogger.d(
      TAG, "onVideoInputFormatChanged: new format: bitrate ${format.bitrate}" +
          " and frameRate ${format.frameRate} "
    )

    // Situations like looping or ad breaks can result in this callback being called for the same
    //  format multiple times over the course of a View. These aren't really rendition changes, and
    //  are not abr-related so we ignore them in this case
    if (shouldReportVideoFormat(eventTime, format)) {
      val cleanBitrate = format.bitrate.takeIf { it >= 0 } ?: 0
      val cleanFrameRate = format.frameRate.takeIf { it >= 0 } ?: 0F

      collector.renditionChange(
        advertisedBitrate = cleanBitrate,
        advertisedFrameRate = cleanFrameRate,
        sourceWidth = format.width,
        sourceHeight = format.height
      )

      this.lastVideoFormat = format
    }
  }

  override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
    MuxLogger.d("ExoPlayerBinding", "onTracksChanged")

    player?.let {
      collector.watchPlayerPos(it)
      collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
    }
    bandwidthMetrics?.onTracksChanged(tracks)
  }

  override fun onDownstreamFormatChanged(
    eventTime: AnalyticsListener.EventTime,
    mediaLoadData: MediaLoadData
  ) {
    if (collector.detectMimeType) {
      mediaLoadData.trackFormat?.containerMimeType?.let { collector.mimeType = it }
    }
  }

  override fun onRenderedFirstFrame(
    eventTime: AnalyticsListener.EventTime,
    output: Any,
    renderTimeMs: Long
  ) {
    collector.onFirstFrameRendered()
  }

  override fun onDroppedVideoFrames(
    eventTime: AnalyticsListener.EventTime,
    droppedFrames: Int,
    elapsedMs: Long
  ) {
    collector.incrementDroppedFrames(droppedFrames)
  }

  override fun onVideoSizeChanged(
    eventTime: AnalyticsListener.EventTime,
    videoSize: VideoSize
  ) {
    val relevant = eventTime.mediaPeriodId?.isInAdGroup() == false
    if (relevant) {
      MuxLogger.d("ExoPlayerBinding", "sizeChanged: change was relevant, setting dimensions")
      collector.sourceWidth = videoSize.width
      collector.sourceHeight = videoSize.height
    }
  }

  override fun onLoadError(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData,
    error: IOException,
    wasCanceled: Boolean
  ) {
    bandwidthMetrics?.onLoadError(
      loadTaskId = loadEventInfo.loadTaskId,
      segmentUrl = loadEventInfo.uri.path,
      e = error
    )
  }

  override fun onLoadCanceled(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {
    bandwidthMetrics?.onLoadCanceled(
      loadTaskId = loadEventInfo.loadTaskId,
      segmentUrl = loadEventInfo.uri.path,
      headers = loadEventInfo.responseHeaders
    )
  }

  override fun onLoadStarted(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {
    var segmentMimeType = "unknown"
    var segmentWidth = 0
    var segmentHeight = 0

    mediaLoadData.trackFormat?.let { format ->
      format.sampleMimeType?.let { segmentMimeType = it }
      segmentWidth = format.width
      segmentHeight = format.height
    }
    bandwidthMetrics?.onLoadStarted(
      loadEventInfo.loadTaskId, mediaLoadData.mediaStartTimeMs,
      mediaLoadData.mediaEndTimeMs, loadEventInfo.uri.path, mediaLoadData.dataType,
      loadEventInfo.uri.host, segmentMimeType, segmentWidth, segmentHeight
    )
  }

  private fun trackTypeString(trackType: Int): String = when (trackType) {
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

  private fun dataTypeString(dataType: Int): String = when (dataType) {
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

  override fun onLoadCompleted(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {

    // for CMAF HLS:
    //  trackType = DEFAULT for video chunks
    //  trackType = AUDIO for audio chunks
    //  DATA_TYPE = media
    // for (not-CMAF) HLS:
    //  trackType = default for all chunks
    //  DATA_TYPE = media
    // in both cases: MVPs and media PLs are dataType MANIFEST and trackType UNKNOWN

    // for CMAF DASH:
    //  trackType = VIDEO for video
    //  trackType = AUDIO for audio
    // Manifests (.mpd) have dataType MANIFEST and trackType audio
    // ** couldn't find any DASH streams with muxed segments, but that isn't common anymore anyway

    Log.d(TAG, "onLoadCompleted: for url: ${loadEventInfo.uri}")
    Log.d(TAG, "onLoadCompleted: Track Type is ${trackTypeString(mediaLoadData.trackType)}")
    Log.d(TAG, "onLoadCompleted: Data Type is ${dataTypeString(mediaLoadData.dataType)}")
    Log.d(TAG, "onLoadCompleted: --")

    @Suppress("SENSELESS_COMPARISON")
    if (loadEventInfo.uri != null) {
      bandwidthMetrics?.onLoadCompleted(
        loadEventInfo.loadTaskId,
        loadEventInfo.uri.path,
        loadEventInfo.bytesLoaded,
        mediaLoadData.trackFormat,
        loadEventInfo.responseHeaders
      )
    }
  } // fun onLoadCompleted

  private fun shouldReportVideoFormat(
    eventTime: AnalyticsListener.EventTime, format: Format
  ): Boolean {
    val formatChanged = this.lastVideoFormat == null || format != this.lastVideoFormat
    val isAdRelated = eventTime.mediaPeriodId?.isInAdGroup() == true

    return formatChanged && !isAdRelated
  }

  companion object {
    private const val TAG = "ExoPlayerBinding"
  }

  init {
    MuxLogger.d(TAG, "Listening to ExoPlayer $player")
  }
}
