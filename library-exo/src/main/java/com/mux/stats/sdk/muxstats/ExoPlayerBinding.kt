package com.mux.stats.sdk.muxstats

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C.TRACK_TYPE_VIDEO
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
import androidx.media3.exoplayer.source.TrackGroupArray
import com.mux.android.util.weak
import com.mux.stats.sdk.core.model.BandwidthMetricData
import com.mux.stats.sdk.core.model.BandwidthMetricData.Rendition
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.bandwidth.BandwidthMetricDispatcher
import java.io.IOException

class ExoPlayerBinding : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {

  private var listener: MuxAnalyticsListener? = null

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener = MuxAnalyticsListener(
      player = player,
      collector = collector,
      bandwidthMetrics = BandwidthMetricDispatcher(
        player = player,
        collector = collector
      )
    ).also { player.addAnalyticsListener(it) }
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener?.let { player.removeAnalyticsListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null
  }

  companion object {
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
              " and framerate ${format.frameRate} "
    )
    val cleanBitrate = if (format.bitrate >= 0) {
      format.bitrate
    } else {
      0
    }
    val cleanFrameRate = if (format.frameRate >= 0) {
      format.frameRate
    } else {
      0
    }
    collector.renditionChange(
      advertisedBitrate = format.averageBitrate,
      advertisedFrameRate = format.frameRate,
      sourceWidth = format.width,
      sourceHeight = format.height
    )
  }

  // TODO: Move this into a util some place
  fun <R> Tracks.Group.mapFormats(block: (Format) -> R): List<R> {
    val retList = mutableListOf<R>()
    for (i in 0 until length) {
      retList.add(block(getTrackFormat(i)))
    }
    return retList
  }

  override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
    MuxLogger.d("ExoPlayerBinding", "onTracksChanged")

    player?.let {
      collector.watchPlayerPos(it)
      collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
    }
    bandwidthMetrics?.let { bwm ->
      tracks.groups.onEach {
        Log.d("ExoPlayerBinding", "group type ${it.type}")
        for (i in 0 until it.length) {
          val format = it.getTrackFormat(i)
          Log.d("ExoPlayerBinding", "format of track $format")
        }
      }
      tracks.groups.filter { it.type == TRACK_TYPE_VIDEO }
        .onEach { Log.d("ExoPlayerBinding", "I'm a video track group") }
        .map { group ->
          group.mapFormats { trackFormat ->
            Rendition().apply {
              bitrate = trackFormat.bitrate.toLong()
              width = trackFormat.width
              height = trackFormat.height
              codec = trackFormat.codecs
              fps = trackFormat.frameRate
              name = trackFormat.width.toString() + "_" +
                      trackFormat.height + "_" +
                      trackFormat.bitrate + "_" + trackFormat.codecs + "_" +
                      trackFormat.frameRate
            }
          }
        }
        .fold(mutableListOf<Rendition>()) { acc, renditionsInGroup ->
          acc.addAll(renditionsInGroup)
          acc
        }
        .also { Log.d("ExoPlayerBinding", "List of video renditions: $it") }

      // TODO: This as-array thing isn't needed
      val mediaTrackGroups = tracks.groups.map { it.mediaTrackGroup }
      val asArray = Array(mediaTrackGroups.size) { mediaTrackGroups[it] }
      bwm.onTracksChanged(TrackGroupArray(*asArray))
    }
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
