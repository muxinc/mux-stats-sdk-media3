package com.mux.stats.media3.test.tools.testdoubles

import com.mux.android.util.logTag
import com.mux.core_android.test.tools.log
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
