package com.mux.stats.sdk.muxstats.internal

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector
import com.mux.stats.sdk.muxstats.util.weak
import java.lang.ref.WeakReference

/**
 * Creates a new instance of the generic Media3 PlayerBinding. Will work with any [Player],
 * including a MediaController
 */
internal fun media3GenericBinding(): MuxPlayerAdapter.PlayerBinding<Player> = Media3PlayerBinding()

/**
 * PlayerBinding for a generic Media3 [Player]
 */
private class Media3PlayerBinding : MuxPlayerAdapter.PlayerBinding<Player> {

  private var listener: MuxPlayerListener? = null

  override fun bindPlayer(player: Player, collector: MuxStateCollector) {
    listener = MuxPlayerListener(player, collector).also { player.addListener(it) }
  }

  override fun unbindPlayer(player: Player, collector: MuxStateCollector) {
    listener?.let { player.removeListener(it) }
    collector.playerWatcher?.stop("player unbound")
    listener = null
  }

}

private class MuxPlayerListener(player: Player, val collector: MuxStateCollector) :
  Player.Listener {
  val player by weak(player)

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

  override fun onTracksChanged(tracks: Tracks) {
    watchPlayerPos(WeakReference(player), collector)
    collector.mediaHasVideoTrack = tracks.hasAtLeastOneVideoTrack()
  }
}
