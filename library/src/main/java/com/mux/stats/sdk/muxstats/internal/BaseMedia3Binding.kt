package com.mux.stats.sdk.muxstats.internal

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import com.mux.android.util.weak
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector

// TODO: Media3PlayerBinding -> open BasicMedia3Binding
//  media3GenericBinding -> detectBinding: Uses Class.forName to decide which binding
//  ... This is a lot.
//  MuxStatsSdkMedia3<P : Player> is a start. Now we for sure only need one big facade
//    What about Player.monitorWithMuxData? Easy! The base doesn't have one
//    The extensions are only for the exo version, and they can be instance-aware

/**
 * Creates a new instance of the generic Media3 PlayerBinding. Will work with any [Player],
 * including a MediaController
 */
fun media3GenericBinding(): MuxPlayerAdapter.PlayerBinding<Player> = BaseMedia3Binding()

/**
 * PlayerBinding for a generic Media3 [Player]
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
      watchPlayerPos(it, collector)
      collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
    }
  }
}
