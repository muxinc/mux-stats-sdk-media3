package com.mux.stats.sdk.muxstats

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import com.mux.android.util.weak

/**
 * PlayerBinding for a generic Media3 [Player]. It reports basic quality information, view timeline
 * events, network and media metadata.
 *
 * You don't ordinarily need to work with this class directly, unless you are implementing your own
 * [Player] with custom APIs, and you wish to observe it
 *
 * If you are using ExoPlayer, you don't need to use this class. Prefer using `ExoPlayerBinding` from
 * the `data-media3` library
 */
open class BaseMedia3Binding<P: Player> : MuxPlayerAdapter.PlayerBinding<P> {

  private var listener: MuxPlayerListener? = null

  override fun bindPlayer(player: P, collector: MuxStateCollector) {
    listener = MuxPlayerListener(player, collector).also { player.addListener(it) }
  }

  override fun unbindPlayer(player: P, collector: MuxStateCollector) {
    listener?.let { player.removeListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null
  }
}

private class MuxPlayerListener(player: Player, val collector: MuxStateCollector) :
  Player.Listener {

  private val player by weak(player)

  override fun onPlaybackStateChanged(playbackState: Int) {
    // We rely on the player's playWhenReady because the order of this callback and its callback
    //  is not well-defined
    player?.let { collector.handleExoPlaybackState(playbackState, it.playWhenReady) }
  }

  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int
  ) {
    collector.handlePositionDiscontinuity(reason)
  }

  override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    timeline.takeIf { it.windowCount > 0 }?.let { tl ->
      val window = Timeline.Window().apply { tl.getWindow(0, this) }
      collector.sourceDurationMs = window.durationMs
    }
  }

  override fun onVideoSizeChanged(videoSize: VideoSize) {
    if (videoSize.height > 0 && videoSize.width > 0) {
      collector.renditionChange(
        sourceHeight = videoSize.height,
        sourceWidth = videoSize.width,
        advertisedBitrate = 0, // The base player does not provide this information
        advertisedFrameRate = 0F, // The base player does not provide this information
      )
    }
  }

  override fun onTracksChanged(tracks: Tracks) {
    player?.let {
      collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
    }
  }
}