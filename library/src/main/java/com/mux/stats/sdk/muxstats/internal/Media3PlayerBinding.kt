package com.mux.stats.sdk.muxstats.internal

import androidx.media3.common.Player
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector

/**
 * Creates a new instance of the generic Media3 PlayerBinding. Will work with any [Player],
 * including a MediaController
 */
internal fun media3GenericBinding(): MuxPlayerAdapter.PlayerBinding<Player> = Media3PlayerBinding()

/**
 *
 */
private class Media3PlayerBinding : MuxPlayerAdapter.PlayerBinding<Player> {

  private var listener: MuxPlayerListener? = null

  override fun bindPlayer(player: Player, collector: MuxStateCollector) {
    player.addListener(MuxPlayerListener(collector))
  }

  override fun unbindPlayer(player: Player, collector: MuxStateCollector) {
    listener?.let { player.removeListener(it) }
    listener = null
  }

}

private class MuxPlayerListener(val collector: MuxStateCollector) : Player.Listener {
}
