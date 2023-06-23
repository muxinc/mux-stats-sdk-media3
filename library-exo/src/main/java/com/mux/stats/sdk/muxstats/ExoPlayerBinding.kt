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
import androidx.media3.exoplayer.source.MediaLoadData
import com.mux.android.util.weak

class ExoPlayerBinding : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {

  private var listener: MuxAnalyticsListener? = null

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener = MuxAnalyticsListener(player, collector).also { player.addAnalyticsListener(it) }
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    listener?.let { player.removeAnalyticsListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null
  }
}

@OptIn(UnstableApi::class)
private class MuxAnalyticsListener(player: ExoPlayer, val collector: MuxStateCollector)
  : AnalyticsListener {
    private val player by weak(player)

  override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
    // We rely on the player's playWhenReady because the order of this callback and its callback
    //  is not well-defined
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
    collector.renditionChange(
      advertisedBitrate = format.averageBitrate,
      advertisedFrameRate = format.frameRate,
      sourceWidth = format.width,
      sourceHeight = format.height
    )
  }

  override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
    player?.let {
      collector.watchPlayerPos(it)
      collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
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

  override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
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
}
