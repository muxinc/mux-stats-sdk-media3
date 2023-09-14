package com.mux.stats.sdk.media3_ima.testdoubles

import com.mux.android.util.logTag
import com.mux.stats.sdk.media3_ima.log
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector

class FakePlayerBinding<Player>(val name: String) : MuxPlayerAdapter.PlayerBinding<Player> {
  override fun bindPlayer(player: Player, collector: MuxStateCollector) {
    log(logTag(), "Binding $name: bindPlayer() called")
  }

  override fun unbindPlayer(player: Player, collector: MuxStateCollector) {
    log(logTag(), "Binding $name: unbindPlayer() called")
  }
}
