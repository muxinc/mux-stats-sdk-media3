package com.mux.stats.sdk.muxstats

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
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

private class MuxAnalyticsListener(player: ExoPlayer, val collector: MuxStateCollector)
  : AnalyticsListener {

    private val player by weak(player)
}
