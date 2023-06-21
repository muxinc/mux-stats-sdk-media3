package com.mux.stats.media3

import android.app.Activity
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

class SomeClass : Activity() {
  fun aFunction() {
    val s = MuxStatsSdkMedia3(
      this,
      envKey = "",
      customerData = CustomerData(),
      player = ExoPlayer.Builder(this).build(),
      playerView = null,
      customOptions = null,
      playerBinding = PB()
    )
  }

  class BPB : MuxPlayerAdapter.PlayerBinding<Player> {
    override fun bindPlayer(player: Player, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

    override fun unbindPlayer(player: Player, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

  }

  class PB : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {
    override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

    override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

  }
}