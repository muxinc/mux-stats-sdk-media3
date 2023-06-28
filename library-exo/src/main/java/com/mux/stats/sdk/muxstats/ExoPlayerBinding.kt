package com.mux.stats.sdk.muxstats

import androidx.annotation.OptIn
import androidx.media3.common.Format
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
import com.mux.stats.sdk.muxstats.internal.createExoSessionDataBinding
import java.io.IOException
import java.util.regex.Pattern

class ExoPlayerBinding : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {

  private val sessionDataBinding = createExoSessionDataBinding()

  private var listener: MuxAnalyticsListener? = null

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
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
    sessionDataBinding.bindPlayer(player, collector)
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener?.let { player.removeAnalyticsListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null

    // Also delegate to sub-bindings
    sessionDataBinding.unbindPlayer(player, collector)
  }

  companion object {
    @Suppress("unused")
    private const val TAG = "ExoPlayerBinding"
  }

}

@OptIn(UnstableApi::class)
private class MuxAnalyticsListener(
  player: ExoPlayer,
  bandwidthMetrics: BandwidthMetricDispatcher,
  val collector: MuxStateCollector,
) : AnalyticsListener {

  private val bandwidthMetrics by weak(bandwidthMetrics)
  private val player by weak(player)

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
    }
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
    val cleanBitrate = format.bitrate.takeIf { it >= 0 } ?: 0
    val cleanFrameRate = format.frameRate.takeIf { it >= 0 } ?: 0F

    collector.renditionChange(
      advertisedBitrate = cleanBitrate,
      advertisedFrameRate = cleanFrameRate,
      sourceWidth = format.width,
      sourceHeight = format.height
    )
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
    collector.sourceWidth = videoSize.width
    collector.sourceHeight = videoSize.height
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

  override fun onLoadCompleted(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData
  ) {
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

  companion object {
    private const val TAG = "ExoPlayerBinding"
  }

  init {
    MuxLogger.d(TAG, "Listening to ExoPlayer $player")
  }
}
